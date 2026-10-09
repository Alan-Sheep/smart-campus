package com.campus.smart.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.smart.common.Result;
import com.campus.smart.entity.Activity;
import com.campus.smart.entity.ActivitySignup;
import com.campus.smart.common.PageQuery;
import com.campus.smart.common.ReqParams;
import com.campus.smart.service.ActivityService;
import com.campus.smart.service.ActivitySignupService;
import com.campus.smart.utils.RedisUtils;
import com.campus.smart.utils.SchoolContext;
import com.campus.smart.utils.UserContext;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 校园活动模块控制器
 */
@Tag(name = "校园活动")
@RestController
@RequestMapping("/api/activity")
public class ActivityController {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private ActivitySignupService activitySignupService;

    @Autowired
    private RedisUtils redisUtils;

    /**
     * 活动列表（分页+按类型筛选）
     */
    @Operation(summary = "活动列表")
    @GetMapping("/list")
    public Result<Page<Activity>> list(PageQuery pageQuery,
                                        @RequestParam(required = false) String type,
                                        @RequestParam(required = false) String keyword) {
        Long schoolId = SchoolContext.getSchoolId();
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();

        if (schoolId != null) {
            wrapper.eq(Activity::getSchoolId, schoolId);
        }
        // type 兼容：传 "all" 或空表示全部，传数字按类型过滤
        if (type != null && !type.isEmpty() && !"all".equalsIgnoreCase(type)) {
            try {
                wrapper.eq(Activity::getActivityType, Integer.parseInt(type));
            } catch (NumberFormatException e) {
                // 格式不对就忽略类型过滤
            }
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Activity::getTitle, keyword);
        }

        // 状态：已发布
        wrapper.eq(Activity::getStatus, 1);
        wrapper.orderByDesc(Activity::getCreateTime);

        Page<Activity> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        Page<Activity> result = activityService.page(page, wrapper);
        return Result.success(result);
    }

    /**
     * 活动详情
     * 加入 Redis 浏览量计数：每次访问详情，Redis 自增 1
     * 返回的浏览量 = 数据库基础值 + Redis 增量（实时+准确）
     */
    @Operation(summary = "活动详情")
    @GetMapping("/detail")
    public Result<Activity> detail(@RequestParam Long id) {
        Activity activity = activityService.getById(id);
        if (activity == null) {
            return Result.error("活动不存在");
        }

        // Redis 浏览量自增，并合并返回
        String viewKey = "campus:activity:view:" + id;
        try {
            // 使用 Redis increment 自增浏览量
            long incrementCount = redisUtils.increment(viewKey);

            // 如果 increment 返回 -1 表示 Redis 操作失败，直接返回数据库的值
            if (incrementCount >= 0) {
                // 浏览量 = 数据库的基础值 + Redis 中的增量
                int baseView = activity.getViewCount() != null ? activity.getViewCount() : 0;
                activity.setViewCount(baseView + (int) incrementCount);
            }
        } catch (Exception e) {
            // Redis 操作失败，不影响主业务，返回数据库中的原始浏览量
            System.err.println("活动浏览量计数异常: " + e.getMessage());
        }

        return Result.success(activity);
    }

    /**
     * 发布活动
     */
    @Operation(summary = "发布活动")
    @PostMapping("/create")
    public Result<Activity> create(@RequestBody Activity activity) {
        Long userId = UserContext.getUserId();
        Long schoolId = SchoolContext.getSchoolId();

        activity.setUserId(userId);
        activity.setSchoolId(schoolId);
        // 状态语义（库注释）：0:已结束 1:报名中 2:进行中。
        // 原实现写入 0（已结束），而 list 只查 status=1，导致「发布后列表看不到」，故改为 1（报名中）。
        activity.setStatus(1);
        // 审核独立走 audit_status（0:待审核 1:已通过 2:已驳回）
        if (activity.getAuditStatus() == null) {
            activity.setAuditStatus(0);
        }
        // 报名人数初始为0
        activity.setCurrentParticipants(0);

        activityService.save(activity);
        return Result.success(activity);
    }

    /**
     * 报名活动
     */
    @Operation(summary = "报名活动")
    @PostMapping("/signup")
    public Result<Void> signup(HttpServletRequest request) {
        Long activityId = ReqParams.requireLong(request, "activityId");
        Long userId = UserContext.getUserId();

        // 检查是否已报名
        LambdaQueryWrapper<ActivitySignup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ActivitySignup::getActivityId, activityId);
        wrapper.eq(ActivitySignup::getUserId, userId);
        ActivitySignup existing = activitySignupService.getOne(wrapper);
        if (existing != null) {
            return Result.error("您已报名此活动");
        }

        // 检查活动是否存在
        Activity activity = activityService.getById(activityId);
        if (activity == null) {
            return Result.error("活动不存在");
        }

        // 创建报名记录
        ActivitySignup signup = new ActivitySignup();
        signup.setActivityId(activityId);
        signup.setUserId(userId);
        activitySignupService.save(signup);

        // 更新报名人数
        Activity update = new Activity();
        update.setId(activityId);
        update.setCurrentParticipants(activity.getCurrentParticipants() + 1);
        activityService.updateById(update);

        return Result.success();
    }

    /**
     * 取消报名
     */
    @Operation(summary = "取消报名")
    @PostMapping("/cancelSignup")
    public Result<Void> cancelSignup(HttpServletRequest request) {
        Long activityId = ReqParams.requireLong(request, "activityId");
        Long userId = UserContext.getUserId();

        LambdaQueryWrapper<ActivitySignup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ActivitySignup::getActivityId, activityId);
        wrapper.eq(ActivitySignup::getUserId, userId);
        ActivitySignup signup = activitySignupService.getOne(wrapper);
        if (signup == null) {
            return Result.error("您未报名此活动");
        }

        // 删除报名记录
        activitySignupService.removeById(signup.getId());

        // 更新报名人数
        Activity activity = activityService.getById(activityId);
        if (activity != null) {
            Activity update = new Activity();
            update.setId(activityId);
            update.setCurrentParticipants(Math.max(0, activity.getCurrentParticipants() - 1));
            activityService.updateById(update);
        }

        return Result.success();
    }

    /**
     * 我的报名列表
     */
    @Operation(summary = "我的报名列表")
    @GetMapping("/mySignup")
    public Result<Page<Activity>> mySignup(PageQuery pageQuery) {
        Long userId = UserContext.getUserId();

        // 查询我的报名记录
        LambdaQueryWrapper<ActivitySignup> signupWrapper = new LambdaQueryWrapper<>();
        signupWrapper.eq(ActivitySignup::getUserId, userId);
        signupWrapper.orderByDesc(ActivitySignup::getCreateTime);

        Page<ActivitySignup> signupPage = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        Page<ActivitySignup> signupResult = activitySignupService.page(signupPage, signupWrapper);

        // 转换为活动列表（简化处理，实际应关联查询）
        Page<Activity> activityPage = new Page<>(signupResult.getCurrent(), signupResult.getSize(), signupResult.getTotal());
        // 这里简化，实际应该通过报名记录关联查询活动
        return Result.success(activityPage);
    }
}
