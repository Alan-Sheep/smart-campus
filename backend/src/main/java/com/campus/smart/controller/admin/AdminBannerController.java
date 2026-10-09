package com.campus.smart.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.smart.common.Result;
import com.campus.smart.entity.Banner;
import com.campus.smart.service.BannerService;
import com.campus.smart.utils.RedisUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端-轮播图管理控制器
 */
@Tag(name = "【管理端】轮播图管理")
@RestController
@RequestMapping("/api/admin/banner")
public class AdminBannerController {

    @Autowired
    private BannerService bannerService;

    @Autowired
    private RedisUtils redisUtils;

    /**
     * 清除首页轮播图缓存
     * 根据 schoolId 删除对应的 Redis 缓存
     *
     * @param schoolId 学校ID
     */
    private void clearBannerCache(Long schoolId) {
        try {
            String schoolKey = schoolId != null ? String.valueOf(schoolId) : "default";
            String cacheKey = "campus:home:banner:" + schoolKey;
            redisUtils.delete(cacheKey);
            // 同时清除 default 的缓存（避免 schoolId 为 null 时的缓存不一致）
            if (schoolId != null) {
                redisUtils.delete("campus:home:banner:default");
            }
        } catch (Exception e) {
            // 缓存清除失败不影响主业务
            System.err.println("清除轮播图缓存异常: " + e.getMessage());
        }
    }

    /**
     * 轮播图列表
     */
    @Operation(summary = "轮播图列表")
    @GetMapping("/list")
    public Result<IPage<Banner>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long schoolId) {

        LambdaQueryWrapper<Banner> wrapper = new LambdaQueryWrapper<>();
        if (schoolId != null) {
            wrapper.eq(Banner::getSchoolId, schoolId);
        }
        wrapper.orderByAsc(Banner::getSort);
        wrapper.orderByDesc(Banner::getCreateTime);

        Page<Banner> page = new Page<>(pageNum, pageSize);
        IPage<Banner> result = bannerService.page(page, wrapper);
        return Result.success(result);
    }

    /**
     * 新增轮播图
     * 新增后清除对应学校的首页轮播图缓存
     */
    @Operation(summary = "新增轮播图")
    @PostMapping("/add")
    public Result<Void> add(@RequestBody Banner banner) {
        if (banner.getTitle() == null || banner.getTitle().isEmpty()) {
            return Result.error("请输入标题");
        }
        if (banner.getImage() == null || banner.getImage().isEmpty()) {
            return Result.error("请上传图片");
        }
        // 默认排序值
        if (banner.getSort() == null) {
            banner.setSort(0);
        }
        // 默认启用
        if (banner.getStatus() == null) {
            banner.setStatus(1);
        }
        bannerService.save(banner);

        // 清除首页轮播图缓存
        clearBannerCache(banner.getSchoolId());

        return Result.success();
    }

    /**
     * 修改轮播图
     * 修改后清除对应学校的首页轮播图缓存
     */
    @Operation(summary = "修改轮播图")
    @PutMapping("/update")
    public Result<Void> update(@RequestBody Banner banner) {
        if (banner.getId() == null) {
            return Result.error("ID不能为空");
        }

        Banner exist = bannerService.getById(banner.getId());
        if (exist == null) {
            return Result.error("轮播图不存在");
        }

        bannerService.updateById(banner);

        // 清除首页轮播图缓存（修改前后的 schoolId 都要清，防止学校ID变更）
        clearBannerCache(exist.getSchoolId());
        if (banner.getSchoolId() != null && !banner.getSchoolId().equals(exist.getSchoolId())) {
            clearBannerCache(banner.getSchoolId());
        }

        return Result.success();
    }

    /**
     * 删除轮播图
     * 删除后清除对应学校的首页轮播图缓存
     */
    @Operation(summary = "删除轮播图")
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Banner banner = bannerService.getById(id);
        if (banner == null) {
            return Result.error("轮播图不存在");
        }

        Long schoolId = banner.getSchoolId();
        bannerService.removeById(id);

        // 清除首页轮播图缓存
        clearBannerCache(schoolId);

        return Result.success();
    }

    /**
     * 启用/禁用轮播图
     * status: 1-启用, 0-禁用
     * 修改状态后清除对应学校的首页轮播图缓存
     */
    @Operation(summary = "启用/禁用轮播图")
    @PutMapping("/updateStatus/{id}")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        Banner banner = bannerService.getById(id);
        if (banner == null) {
            return Result.error("轮播图不存在");
        }

        Banner update = new Banner();
        update.setId(id);
        update.setStatus(status);
        bannerService.updateById(update);

        // 清除首页轮播图缓存
        clearBannerCache(banner.getSchoolId());

        return Result.success();
    }

    /**
     * 更新排序
     * 接收排序后的ID列表，按顺序更新sort值
     * 更新排序后清除所有涉及学校的首页轮播图缓存
     */
    @Operation(summary = "更新排序")
    @PutMapping("/sort")
    public Result<Void> sort(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error("参数错误");
        }

        // 先记录第一个 banner 的 schoolId 用于清缓存（一般同一次排序的都是同一个学校的）
        Long firstSchoolId = null;
        if (!ids.isEmpty()) {
            Banner firstBanner = bannerService.getById(ids.get(0));
            if (firstBanner != null) {
                firstSchoolId = firstBanner.getSchoolId();
            }
        }

        for (int i = 0; i < ids.size(); i++) {
            Banner banner = new Banner();
            banner.setId(ids.get(i));
            banner.setSort(i);
            bannerService.updateById(banner);
        }

        // 清除首页轮播图缓存
        clearBannerCache(firstSchoolId);

        return Result.success();
    }
}
