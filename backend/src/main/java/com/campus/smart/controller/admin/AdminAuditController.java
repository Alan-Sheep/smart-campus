package com.campus.smart.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.smart.common.Result;
import com.campus.smart.entity.Activity;
import com.campus.smart.entity.Comment;
import com.campus.smart.entity.Goods;
import com.campus.smart.entity.LostFound;
import com.campus.smart.entity.Post;
import com.campus.smart.service.ActivityService;
import com.campus.smart.service.CommentService;
import com.campus.smart.service.GoodsService;
import com.campus.smart.service.LostFoundService;
import com.campus.smart.service.PostService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理端-内容审核控制器
 * 包含活动审核、商品审核、失物招领审核、帖子审核
 */
@Tag(name = "【管理端】内容审核")
@RestController
@RequestMapping("/api/admin/audit")
public class AdminAuditController {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private LostFoundService lostFoundService;

    @Autowired
    private PostService postService;

    @Autowired
    private CommentService commentService;

    // ==================== 活动审核 ====================

    /**
     * 活动审核列表
     * 分页 + 审核状态筛选
     */
    @Operation(summary = "活动审核列表")
    @GetMapping("/activityList")
    public Result<IPage<Activity>> activityList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(required = false) Long schoolId) {

        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        if (auditStatus != null) {
            wrapper.eq(Activity::getAuditStatus, auditStatus);
        }
        if (schoolId != null) {
            wrapper.eq(Activity::getSchoolId, schoolId);
        }
        wrapper.orderByDesc(Activity::getCreateTime);

        Page<Activity> page = new Page<>(pageNum, pageSize);
        IPage<Activity> result = activityService.page(page, wrapper);
        return Result.success(result);
    }

    /**
     * 活动详情
     */
    @Operation(summary = "活动详情")
    @GetMapping("/activityDetail/{id}")
    public Result<Activity> activityDetail(@PathVariable Long id) {
        Activity activity = activityService.getById(id);
        if (activity == null) {
            return Result.error("活动不存在");
        }
        return Result.success(activity);
    }

    /**
     * 活动审核
     * auditStatus: 1-通过, 2-驳回
     */
    @Operation(summary = "活动审核通过/驳回")
    @PutMapping("/activityAudit/{id}")
    public Result<Void> activityAudit(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        Activity activity = activityService.getById(id);
        if (activity == null) {
            return Result.error("活动不存在");
        }

        Integer auditStatus = params.get("auditStatus") != null ?
                Integer.valueOf(params.get("auditStatus").toString()) : null;
        String rejectReason = params.get("rejectReason") != null ?
                params.get("rejectReason").toString() : null;

        if (auditStatus == null) {
            return Result.error("请选择审核结果");
        }
        if (auditStatus == 2 && (rejectReason == null || rejectReason.isEmpty())) {
            return Result.error("驳回时请填写驳回原因");
        }

        Activity update = new Activity();
        update.setId(id);
        update.setAuditStatus(auditStatus);
        update.setRejectReason(rejectReason);
        // 审核通过时，活动状态设为上架
        if (auditStatus == 1) {
            update.setStatus(1);
        }
        activityService.updateById(update);
        return Result.success();
    }

    /**
     * 下架活动
     */
    @Operation(summary = "下架活动")
    @PutMapping("/activityRemove/{id}")
    public Result<Void> activityRemove(@PathVariable Long id) {
        Activity activity = activityService.getById(id);
        if (activity == null) {
            return Result.error("活动不存在");
        }

        Activity update = new Activity();
        update.setId(id);
        update.setStatus(0); // 下架
        activityService.updateById(update);
        return Result.success();
    }

    // ==================== 商品审核 ====================

    /**
     * 商品审核列表
     * 分页 + 审核状态筛选
     */
    @Operation(summary = "商品审核列表")
    @GetMapping("/goodsList")
    public Result<IPage<Goods>> goodsList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(required = false) Long schoolId) {

        LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<>();
        if (auditStatus != null) {
            wrapper.eq(Goods::getAuditStatus, auditStatus);
        }
        if (schoolId != null) {
            wrapper.eq(Goods::getSchoolId, schoolId);
        }
        wrapper.orderByDesc(Goods::getCreateTime);

        Page<Goods> page = new Page<>(pageNum, pageSize);
        IPage<Goods> result = goodsService.page(page, wrapper);
        return Result.success(result);
    }

    /**
     * 商品详情
     */
    @Operation(summary = "商品详情")
    @GetMapping("/goodsDetail/{id}")
    public Result<Goods> goodsDetail(@PathVariable Long id) {
        Goods goods = goodsService.getById(id);
        if (goods == null) {
            return Result.error("商品不存在");
        }
        return Result.success(goods);
    }

    /**
     * 商品审核
     * auditStatus: 1-通过, 2-驳回
     */
    @Operation(summary = "商品审核通过/驳回")
    @PutMapping("/goodsAudit/{id}")
    public Result<Void> goodsAudit(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        Goods goods = goodsService.getById(id);
        if (goods == null) {
            return Result.error("商品不存在");
        }

        Integer auditStatus = params.get("auditStatus") != null ?
                Integer.valueOf(params.get("auditStatus").toString()) : null;
        String rejectReason = params.get("rejectReason") != null ?
                params.get("rejectReason").toString() : null;

        if (auditStatus == null) {
            return Result.error("请选择审核结果");
        }
        if (auditStatus == 2 && (rejectReason == null || rejectReason.isEmpty())) {
            return Result.error("驳回时请填写驳回原因");
        }

        Goods update = new Goods();
        update.setId(id);
        update.setAuditStatus(auditStatus);
        update.setRejectReason(rejectReason);
        // 审核通过时，商品状态设为上架
        if (auditStatus == 1) {
            update.setStatus(1);
        }
        goodsService.updateById(update);
        return Result.success();
    }

    /**
     * 下架商品
     */
    @Operation(summary = "下架商品")
    @PutMapping("/goodsRemove/{id}")
    public Result<Void> goodsRemove(@PathVariable Long id) {
        Goods goods = goodsService.getById(id);
        if (goods == null) {
            return Result.error("商品不存在");
        }

        Goods update = new Goods();
        update.setId(id);
        update.setStatus(0); // 下架
        goodsService.updateById(update);
        return Result.success();
    }

    // ==================== 失物招领审核 ====================

    /**
     * 失物招领审核列表
     * 分页 + 审核状态筛选
     */
    @Operation(summary = "失物招领审核列表")
    @GetMapping("/lostfoundList")
    public Result<IPage<LostFound>> lostfoundList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Long schoolId) {

        LambdaQueryWrapper<LostFound> wrapper = new LambdaQueryWrapper<>();
        if (auditStatus != null) {
            wrapper.eq(LostFound::getAuditStatus, auditStatus);
        }
        // 类型筛选（0:寻物 1:招领）——原先缺少该参数，前端只能在本页做客户端过滤，
        // 会导致总数与分页不准确，这里补上服务端过滤。
        if (type != null) {
            wrapper.eq(LostFound::getType, type);
        }
        if (schoolId != null) {
            wrapper.eq(LostFound::getSchoolId, schoolId);
        }
        wrapper.orderByDesc(LostFound::getCreateTime);

        Page<LostFound> page = new Page<>(pageNum, pageSize);
        IPage<LostFound> result = lostFoundService.page(page, wrapper);
        return Result.success(result);
    }

    /**
     * 失物招领详情
     */
    @Operation(summary = "失物招领详情")
    @GetMapping("/lostfoundDetail/{id}")
    public Result<LostFound> lostfoundDetail(@PathVariable Long id) {
        LostFound lostFound = lostFoundService.getById(id);
        if (lostFound == null) {
            return Result.error("失物招领信息不存在");
        }
        return Result.success(lostFound);
    }

    /**
     * 失物招领审核
     * auditStatus: 1-通过, 2-驳回
     */
    @Operation(summary = "失物招领审核通过/驳回")
    @PutMapping("/lostfoundAudit/{id}")
    public Result<Void> lostfoundAudit(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        LostFound lostFound = lostFoundService.getById(id);
        if (lostFound == null) {
            return Result.error("失物招领信息不存在");
        }

        Integer auditStatus = params.get("auditStatus") != null ?
                Integer.valueOf(params.get("auditStatus").toString()) : null;
        String rejectReason = params.get("rejectReason") != null ?
                params.get("rejectReason").toString() : null;

        if (auditStatus == null) {
            return Result.error("请选择审核结果");
        }
        if (auditStatus == 2 && (rejectReason == null || rejectReason.isEmpty())) {
            return Result.error("驳回时请填写驳回原因");
        }

        LostFound update = new LostFound();
        update.setId(id);
        update.setAuditStatus(auditStatus);
        update.setRejectReason(rejectReason);
        // 审核通过时，状态设为正常
        if (auditStatus == 1) {
            update.setStatus(1);
        }
        lostFoundService.updateById(update);
        return Result.success();
    }

    /**
     * 下架失物招领
     */
    @Operation(summary = "下架失物招领")
    @PutMapping("/lostfoundRemove/{id}")
    public Result<Void> lostfoundRemove(@PathVariable Long id) {
        LostFound lostFound = lostFoundService.getById(id);
        if (lostFound == null) {
            return Result.error("失物招领信息不存在");
        }

        LostFound update = new LostFound();
        update.setId(id);
        update.setStatus(0); // 下架
        lostFoundService.updateById(update);
        return Result.success();
    }

    // ==================== 帖子审核 ====================

    /**
     * 帖子审核列表
     * 分页 + 审核状态筛选
     */
    @Operation(summary = "帖子审核列表")
    @GetMapping("/postList")
    public Result<IPage<Post>> postList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(required = false) Long schoolId) {

        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        if (auditStatus != null) {
            wrapper.eq(Post::getAuditStatus, auditStatus);
        }
        if (schoolId != null) {
            wrapper.eq(Post::getSchoolId, schoolId);
        }
        wrapper.orderByDesc(Post::getCreateTime);

        Page<Post> page = new Page<>(pageNum, pageSize);
        IPage<Post> result = postService.page(page, wrapper);
        return Result.success(result);
    }

    /**
     * 帖子详情
     */
    @Operation(summary = "帖子详情")
    @GetMapping("/postDetail/{id}")
    public Result<Post> postDetail(@PathVariable Long id) {
        Post post = postService.getById(id);
        if (post == null) {
            return Result.error("帖子不存在");
        }
        return Result.success(post);
    }

    /**
     * 帖子审核
     * auditStatus: 1-通过, 2-驳回
     */
    @Operation(summary = "帖子审核通过/驳回")
    @PutMapping("/postAudit/{id}")
    public Result<Void> postAudit(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        Post post = postService.getById(id);
        if (post == null) {
            return Result.error("帖子不存在");
        }

        Integer auditStatus = params.get("auditStatus") != null ?
                Integer.valueOf(params.get("auditStatus").toString()) : null;
        String rejectReason = params.get("rejectReason") != null ?
                params.get("rejectReason").toString() : null;

        if (auditStatus == null) {
            return Result.error("请选择审核结果");
        }
        if (auditStatus == 2 && (rejectReason == null || rejectReason.isEmpty())) {
            return Result.error("驳回时请填写驳回原因");
        }

        Post update = new Post();
        update.setId(id);
        update.setAuditStatus(auditStatus);
        update.setRejectReason(rejectReason);
        postService.updateById(update);
        return Result.success();
    }

    /**
     * 帖子置顶/取消置顶
     * isTop: 1-置顶, 0-取消置顶
     */
    @Operation(summary = "帖子置顶/取消置顶")
    @PutMapping("/postTop/{id}")
    public Result<Void> postTop(@PathVariable Long id, @RequestParam Integer isTop) {
        Post post = postService.getById(id);
        if (post == null) {
            return Result.error("帖子不存在");
        }

        Post update = new Post();
        update.setId(id);
        update.setIsTop(isTop);
        postService.updateById(update);
        return Result.success();
    }

    /**
     * 删除帖子
     */
    @Operation(summary = "删除帖子")
    @DeleteMapping("/postDelete/{id}")
    public Result<Void> postDelete(@PathVariable Long id) {
        Post post = postService.getById(id);
        if (post == null) {
            return Result.error("帖子不存在");
        }

        postService.removeById(id);
        return Result.success();
    }

    /**
     * 删除评论
     */
    @Operation(summary = "删除评论")
    @DeleteMapping("/commentDelete/{id}")
    public Result<Void> commentDelete(@PathVariable Long id) {
        Comment comment = commentService.getById(id);
        if (comment == null) {
            return Result.error("评论不存在");
        }

        commentService.removeById(id);
        return Result.success();
    }
}
