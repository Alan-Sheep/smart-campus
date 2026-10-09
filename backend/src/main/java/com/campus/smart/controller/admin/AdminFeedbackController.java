package com.campus.smart.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.smart.common.Result;
import com.campus.smart.entity.Feedback;
import com.campus.smart.service.FeedbackService;
import com.campus.smart.utils.AdminContext;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 管理端-反馈管理控制器
 */
@Tag(name = "【管理端】反馈管理")
@RestController
@RequestMapping("/api/admin/feedback")
public class AdminFeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    /**
     * 反馈列表
     * 分页 + 状态 + 类型筛选
     */
    @Operation(summary = "反馈列表")
    @GetMapping("/list")
    public Result<IPage<Feedback>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Long schoolId) {

        LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Feedback::getStatus, status);
        }
        if (type != null) {
            wrapper.eq(Feedback::getType, type);
        }
        if (schoolId != null) {
            wrapper.eq(Feedback::getSchoolId, schoolId);
        }
        wrapper.orderByDesc(Feedback::getCreateTime);

        Page<Feedback> page = new Page<>(pageNum, pageSize);
        IPage<Feedback> result = feedbackService.page(page, wrapper);
        return Result.success(result);
    }

    /**
     * 反馈详情
     */
    @Operation(summary = "反馈详情")
    @GetMapping("/detail/{id}")
    public Result<Feedback> detail(@PathVariable Long id) {
        Feedback feedback = feedbackService.getById(id);
        if (feedback == null) {
            return Result.error("反馈不存在");
        }
        return Result.success(feedback);
    }

    /**
     * 处理反馈（更新状态）
     *
     * <p>状态语义以库表注释为准：0:待处理 1:处理中 2:已完成。
     * （原注释写的是 1/2/3，与库中注释及既有数据不一致，已更正。）</p>
     */
    @Operation(summary = "处理反馈")
    @PutMapping("/handle/{id}")
    public Result<Void> handle(@PathVariable Long id, @RequestParam Integer status) {
        Feedback feedback = feedbackService.getById(id);
        if (feedback == null) {
            return Result.error("反馈不存在");
        }
        if (status == null || status < 0 || status > 2) {
            return Result.error("状态值不合法（0:待处理 1:处理中 2:已完成）");
        }

        Feedback update = new Feedback();
        update.setId(id);
        update.setStatus(status);
        feedbackService.updateById(update);
        return Result.success();
    }

    /**
     * 回复反馈
     *
     * <p>回复即视为处理完成，状态置为 2（已完成）。</p>
     * <p>注意：原实现写死 status=3，而库中 status 语义为 0/1/2，
     * 3 是非法值，导致用户端（按 status 判断是否展示回复）看不到回复内容。</p>
     */
    @Operation(summary = "回复反馈")
    @PutMapping("/reply/{id}")
    public Result<Void> reply(@PathVariable Long id, @RequestBody Map<String, String> params) {
        String reply = params.get("reply");
        if (reply == null || reply.isEmpty()) {
            return Result.error("回复内容不能为空");
        }

        Feedback feedback = feedbackService.getById(id);
        if (feedback == null) {
            return Result.error("反馈不存在");
        }

        Feedback update = new Feedback();
        update.setId(id);
        update.setReply(reply);
        update.setReplyTime(LocalDateTime.now());
        update.setReplyAdminId(AdminContext.getAdminId());
        update.setStatus(2); // 已完成（0:待处理 1:处理中 2:已完成）
        feedbackService.updateById(update);
        return Result.success();
    }
}
