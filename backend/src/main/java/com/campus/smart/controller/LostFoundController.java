package com.campus.smart.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.smart.common.Result;
import com.campus.smart.entity.LostFound;
import com.campus.smart.common.PageQuery;
import com.campus.smart.common.ReqParams;
import com.campus.smart.service.LostFoundService;
import com.campus.smart.utils.SchoolContext;
import com.campus.smart.utils.UserContext;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 失物招领模块控制器
 */
@Tag(name = "失物招领")
@RestController
@RequestMapping("/api/lostfound")
public class LostFoundController {

    @Autowired
    private LostFoundService lostFoundService;

    /**
     * 列表（按类型筛选：寻物/招领）
     * type: 1-寻物启事, 2-失物招领
     */
    @Operation(summary = "失物招领列表")
    @GetMapping("/list")
    public Result<Page<LostFound>> list(PageQuery pageQuery,
                                         @RequestParam(required = false) String type,
                                         @RequestParam(required = false) String keyword) {
        Long schoolId = SchoolContext.getSchoolId();
        LambdaQueryWrapper<LostFound> wrapper = new LambdaQueryWrapper<>();

        if (schoolId != null) {
            wrapper.eq(LostFound::getSchoolId, schoolId);
        }
        if (type != null && !type.isEmpty() && !"all".equalsIgnoreCase(type)) {
            // 兼容：支持 lost/found 文字和 0/1 数字（type 列为 TINYINT）
            String typeValue = type;
            if ("lost".equalsIgnoreCase(type)) {
                typeValue = "0";
            } else if ("found".equalsIgnoreCase(type)) {
                typeValue = "1";
            }
            try {
                wrapper.eq(LostFound::getType, Integer.valueOf(typeValue));
            } catch (NumberFormatException e) {
                // 非法类型值：忽略过滤，避免 500
            }
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(LostFound::getItemName, keyword);
        }

        // 列表展示全部（不再只查 status=1）：
        // 原先 status=1 表示「已解决」，而新建记录 status=0，导致刚发布的内容列表看不到。
        // 审核过滤仍由 audit_status 承担（已通过=1）。
        wrapper.eq(LostFound::getAuditStatus, 1);
        wrapper.orderByDesc(LostFound::getCreateTime);

        Page<LostFound> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        Page<LostFound> result = lostFoundService.page(page, wrapper);
        return Result.success(result);
    }

    /**
     * 详情
     */
    @Operation(summary = "失物招领详情")
    @GetMapping("/detail")
    public Result<LostFound> detail(@RequestParam Long id) {
        LostFound lostFound = lostFoundService.getById(id);
        if (lostFound == null) {
            return Result.error("记录不存在");
        }
        return Result.success(lostFound);
    }

    /**
     * 发布
     */
    @Operation(summary = "发布失物招领")
    @PostMapping("/create")
    public Result<LostFound> create(@RequestBody LostFound lostFound) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        Long schoolId = SchoolContext.getSchoolId();

        lostFound.setUserId(userId);
        lostFound.setSchoolId(schoolId);
        // 状态语义（库注释 + 前端 statusText 一致）：0:未解决（寻找中/待认领） 1:已解决（已找到/已认领）。
        // 原实现写入 0 但 list 只查 status=1，导致「发布后列表看不到」；
        // 这里保持语义正确的 0，并改为 list 不再按 status 过滤（见 list 方法）。
        lostFound.setStatus(0);
        // 类型缺省为 0（寻物）
        if (lostFound.getType() == null) {
            lostFound.setType(0);
        }

        lostFoundService.save(lostFound);
        return Result.success(lostFound);
    }

    /**
     * 我发布的
     */
    @Operation(summary = "我发布的")
    @GetMapping("/myList")
    public Result<Page<LostFound>> myList(PageQuery pageQuery,
                                           @RequestParam(required = false) String type) {
        Long userId = UserContext.getUserId();
        LambdaQueryWrapper<LostFound> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LostFound::getUserId, userId);

        if (type != null && !type.isEmpty()) {
            try {
                wrapper.eq(LostFound::getType, Integer.valueOf(type));
            } catch (NumberFormatException e) {
                // 非法类型值：忽略过滤
            }
        }

        wrapper.orderByDesc(LostFound::getCreateTime);

        Page<LostFound> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        Page<LostFound> result = lostFoundService.page(page, wrapper);
        return Result.success(result);
    }

    /**
     * 更新状态（标记已解决/未解决）
     * status: 1-已发布/未解决, 2-已解决
     */
    @Operation(summary = "更新状态")
    @PostMapping("/updateStatus")
    public Result<Void> updateStatus(HttpServletRequest request) {
        Long id = ReqParams.requireLong(request, "id");
        Integer status = ReqParams.getInteger(request, "status");
        if (status == null) {
            throw new IllegalArgumentException("缺少必要参数：status");
        }
        Long userId = UserContext.getUserId();
        LostFound lostFound = lostFoundService.getById(id);
        if (lostFound == null) {
            return Result.error("记录不存在");
        }
        if (!lostFound.getUserId().equals(userId)) {
            return Result.error("只能操作自己发布的内容");
        }

        LostFound update = new LostFound();
        update.setId(id);
        update.setStatus(status);
        lostFoundService.updateById(update);
        return Result.success();
    }
}
