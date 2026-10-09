package com.campus.smart.controller;

import com.campus.smart.common.Result;
import com.campus.smart.entity.Feedback;
import com.campus.smart.service.FeedbackService;
import com.campus.smart.utils.SchoolContext;
import com.campus.smart.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Tag(name = "意见反馈")
@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @Operation(summary = "提交反馈")
    @PostMapping("/submit")
    public Result<String> submit(@RequestBody Feedback feedback) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        Long schoolId = SchoolContext.getSchoolId();
        feedback.setUserId(userId);
        feedback.setSchoolId(schoolId);
        feedback.setStatus(0); // 0=待处理
        // 反馈类型缺省为 0（功能建议）；列类型为 TINYINT
        if (feedback.getType() == null) {
            feedback.setType(0);
        }
        feedbackService.save(feedback);
        return Result.success("提交成功");
    }

    @Operation(summary = "我的反馈列表")
    @GetMapping("/myList")
    public Result<?> myList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        return Result.success(
            feedbackService.lambdaQuery()
                .eq(Feedback::getUserId, userId)
                .orderByDesc(Feedback::getCreateTime)
                .page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize))
        );
    }
}
