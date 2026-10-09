package com.campus.smart.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.smart.common.Result;
import com.campus.smart.entity.Notice;
import com.campus.smart.service.NoticeService;
import com.campus.smart.utils.RedisUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端-公告管理控制器
 */
@Tag(name = "【管理端】公告管理")
@RestController
@RequestMapping("/api/admin/notice")
public class AdminNoticeController {

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private RedisUtils redisUtils;

    /**
     * 清除首页公告缓存
     * 根据 schoolId 删除对应的 Redis 缓存
     *
     * @param schoolId 学校ID
     */
    private void clearNoticeCache(Long schoolId) {
        try {
            String schoolKey = schoolId != null ? String.valueOf(schoolId) : "default";
            String cacheKey = "campus:home:notice:" + schoolKey;
            redisUtils.delete(cacheKey);
            // 同时清除 default 的缓存（避免 schoolId 为 null 时的缓存不一致）
            if (schoolId != null) {
                redisUtils.delete("campus:home:notice:default");
            }
        } catch (Exception e) {
            // 缓存清除失败不影响主业务
            System.err.println("清除公告缓存异常: " + e.getMessage());
        }
    }

    /**
     * 公告列表
     */
    @Operation(summary = "公告列表")
    @GetMapping("/list")
    public Result<IPage<Notice>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Long schoolId) {

        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        if (title != null && !title.isEmpty()) {
            wrapper.like(Notice::getTitle, title);
        }
        if (schoolId != null) {
            wrapper.eq(Notice::getSchoolId, schoolId);
        }
        // 置顶的排前面
        wrapper.orderByDesc(Notice::getIsTop);
        wrapper.orderByDesc(Notice::getCreateTime);

        Page<Notice> page = new Page<>(pageNum, pageSize);
        IPage<Notice> result = noticeService.page(page, wrapper);
        return Result.success(result);
    }

    /**
     * 新增公告
     * 新增后清除对应学校的首页公告缓存
     */
    @Operation(summary = "新增公告")
    @PostMapping("/add")
    public Result<Void> add(@RequestBody Notice notice) {
        if (notice.getTitle() == null || notice.getTitle().isEmpty()) {
            return Result.error("请输入标题");
        }
        if (notice.getContent() == null || notice.getContent().isEmpty()) {
            return Result.error("请输入内容");
        }
        // 默认值
        if (notice.getIsTop() == null) {
            notice.setIsTop(0);
        }
        if (notice.getSort() == null) {
            notice.setSort(0);
        }
        if (notice.getStatus() == null) {
            notice.setStatus(1);
        }
        noticeService.save(notice);

        // 清除首页公告缓存
        clearNoticeCache(notice.getSchoolId());

        return Result.success();
    }

    /**
     * 修改公告
     * 修改后清除对应学校的首页公告缓存
     */
    @Operation(summary = "修改公告")
    @PutMapping("/update")
    public Result<Void> update(@RequestBody Notice notice) {
        if (notice.getId() == null) {
            return Result.error("ID不能为空");
        }

        Notice exist = noticeService.getById(notice.getId());
        if (exist == null) {
            return Result.error("公告不存在");
        }

        noticeService.updateById(notice);

        // 清除首页公告缓存（修改前后的 schoolId 都要清，防止学校ID变更）
        clearNoticeCache(exist.getSchoolId());
        if (notice.getSchoolId() != null && !notice.getSchoolId().equals(exist.getSchoolId())) {
            clearNoticeCache(notice.getSchoolId());
        }

        return Result.success();
    }

    /**
     * 删除公告
     * 删除后清除对应学校的首页公告缓存
     */
    @Operation(summary = "删除公告")
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Notice notice = noticeService.getById(id);
        if (notice == null) {
            return Result.error("公告不存在");
        }

        Long schoolId = notice.getSchoolId();
        noticeService.removeById(id);

        // 清除首页公告缓存
        clearNoticeCache(schoolId);

        return Result.success();
    }

    /**
     * 置顶/取消置顶
     * isTop: 1-置顶, 0-取消置顶
     * 修改后清除对应学校的首页公告缓存
     */
    @Operation(summary = "置顶/取消置顶")
    @PutMapping("/top/{id}")
    public Result<Void> top(@PathVariable Long id, @RequestParam Integer isTop) {
        Notice notice = noticeService.getById(id);
        if (notice == null) {
            return Result.error("公告不存在");
        }

        Notice update = new Notice();
        update.setId(id);
        update.setIsTop(isTop);
        noticeService.updateById(update);

        // 清除首页公告缓存
        clearNoticeCache(notice.getSchoolId());

        return Result.success();
    }
}
