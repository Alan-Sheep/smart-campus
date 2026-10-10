package com.campus.smart.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.smart.common.Result;
import com.campus.smart.entity.Collect;
import com.campus.smart.entity.Comment;
import com.campus.smart.entity.LikeRecord;
import com.campus.smart.entity.Post;
import com.campus.smart.common.PageQuery;
import com.campus.smart.common.ReqParams;
import com.campus.smart.mapper.CollectMapper;
import com.campus.smart.service.CollectService;
import com.campus.smart.service.CommentService;
import com.campus.smart.service.LikeRecordService;
import com.campus.smart.service.PostService;
import com.campus.smart.utils.RedisUtils;
import com.campus.smart.utils.SchoolContext;
import com.campus.smart.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 校园频道模块控制器
 */
@Tag(name = "校园频道")
@RestController
@RequestMapping("/api/channel")
public class ChannelController {

    /** 收藏/点赞目标类型：帖子 */
    private static final int TARGET_TYPE_POST = 1;

    @Autowired
    private PostService postService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private LikeRecordService likeRecordService;

    @Autowired
    private CollectService collectService;

    @Autowired
    private CollectMapper collectMapper;

    @Autowired
    private RedisUtils redisUtils;

    /**
     * 帖子列表（分页）
     * 按学校筛选，只显示审核通过的帖子，按创建时间倒序排列
     * 支持关键词搜索（匹配标题或内容）
     */
    @Operation(summary = "帖子列表")
    @GetMapping("/postList")
    public Result<Page<Post>> postList(PageQuery pageQuery,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) String category) {
        Long schoolId = SchoolContext.getSchoolId();
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();

        // 按学校筛选
        if (schoolId != null) {
            wrapper.eq(Post::getSchoolId, schoolId);
        }

        // 关键词搜索：匹配标题或内容
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Post::getTitle, keyword).or().like(Post::getContent, keyword));
        }

        // 分类筛选：推荐/吐槽/交友/分享
        // 「推荐」等价于全部（不过滤），其余按 category 精确匹配。
        if (category != null && !category.isEmpty()
                && !"推荐".equals(category) && !"all".equalsIgnoreCase(category)) {
            wrapper.eq(Post::getCategory, category);
        }

        // 只显示审核通过的帖子（auditStatus = 1）
        wrapper.eq(Post::getAuditStatus, 1);
        // 按创建时间倒序
        wrapper.orderByDesc(Post::getCreateTime);

        Page<Post> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        Page<Post> result = postService.page(page, wrapper);

        // 补充点赞数和评论数
        // 性能优化：原先在 for 循环里对每条帖子各执行 2 次 count()，属于典型 N+1
        //（每页 10 条 = 1 + 20 = 21 次查询）。现改为一次性批量聚合统计，固定 2 次查询。
        if (result.getRecords() != null && !result.getRecords().isEmpty()) {
            List<Long> postIds = result.getRecords().stream().map(Post::getId).collect(Collectors.toList());

            // 批量统计点赞数（单次 GROUP BY 查询）
            LambdaQueryWrapper<LikeRecord> likeWrapper = new LambdaQueryWrapper<>();
            likeWrapper.eq(LikeRecord::getTargetType, "0");
            likeWrapper.in(LikeRecord::getTargetId, postIds);
            likeWrapper.select(LikeRecord::getTargetId);
            List<LikeRecord> allLikes = likeRecordService.list(likeWrapper);
            java.util.Map<Long, Long> likeCountMap = allLikes.stream()
                    .filter(l -> l.getTargetId() != null)
                    .collect(Collectors.groupingBy(LikeRecord::getTargetId, Collectors.counting()));

            // 批量统计评论数（单次 GROUP BY 查询）
            LambdaQueryWrapper<Comment> commentWrapper = new LambdaQueryWrapper<>();
            commentWrapper.in(Comment::getPostId, postIds);
            commentWrapper.select(Comment::getPostId);
            List<Comment> allComments = commentService.list(commentWrapper);
            java.util.Map<Long, Long> commentCountMap = allComments.stream()
                    .filter(c -> c.getPostId() != null)
                    .collect(Collectors.groupingBy(Comment::getPostId, Collectors.counting()));

            for (Post post : result.getRecords()) {
                post.setLikeCount(likeCountMap.getOrDefault(post.getId(), 0L).intValue());
                post.setCommentCount(commentCountMap.getOrDefault(post.getId(), 0L).intValue());
            }

            // 当前用户已点赞的帖子 + 已收藏的帖子
            Long userId = UserContext.getUserId();
            if (userId != null) {
                LambdaQueryWrapper<LikeRecord> userLikeWrapper = new LambdaQueryWrapper<>();
                userLikeWrapper.eq(LikeRecord::getUserId, userId);
                userLikeWrapper.eq(LikeRecord::getTargetType, "0");
                userLikeWrapper.in(LikeRecord::getTargetId, postIds);
                List<LikeRecord> userLikes = likeRecordService.list(userLikeWrapper);
                java.util.Set<Long> likedIds = userLikes.stream()
                        .map(LikeRecord::getTargetId)
                        .collect(Collectors.toSet());

                // 批量查当前用户对这批帖子的收藏记录（targetType=1 表示帖子）
                LambdaQueryWrapper<Collect> collectWrapper = new LambdaQueryWrapper<>();
                collectWrapper.eq(Collect::getUserId, userId);
                collectWrapper.eq(Collect::getTargetType, TARGET_TYPE_POST);
                collectWrapper.in(Collect::getTargetId, postIds);
                java.util.Set<Long> collectedIds = collectService.list(collectWrapper).stream()
                        .map(Collect::getTargetId)
                        .collect(Collectors.toSet());

                for (Post post : result.getRecords()) {
                    post.setIsLiked(likedIds.contains(post.getId()));
                    post.setIsCollected(collectedIds.contains(post.getId()));
                }
            } else {
                for (Post post : result.getRecords()) {
                    post.setIsLiked(false);
                    post.setIsCollected(false);
                }
            }
        }

        return Result.success(result);
    }

    /**
     * 帖子详情
     * 加入 Redis 浏览量计数：每次访问详情，Redis 自增 1
     * 返回的浏览量 = 数据库基础值 + Redis 增量（实时+准确）
     * Redis 操作失败时兜底返回数据库原始值
     */
    @Operation(summary = "帖子详情")
    @GetMapping("/postDetail")
    public Result<Post> postDetail(@RequestParam Long id) {
        Post post = postService.getById(id);
        if (post == null) {
            return Result.error("帖子不存在");
        }

        // Redis 浏览量自增，并合并返回
        String viewKey = "campus:post:view:" + id;
        try {
            // 使用 Redis increment 自增浏览量
            long incrementCount = redisUtils.increment(viewKey);

            // 如果 increment 返回 -1 表示 Redis 操作失败，直接返回数据库的值
            if (incrementCount >= 0) {
                // 浏览量 = 数据库的基础值 + Redis 中的增量
                int baseView = post.getViewCount() != null ? post.getViewCount() : 0;
                post.setViewCount(baseView + (int) incrementCount);
            }
        } catch (Exception e) {
            // Redis 操作失败，不影响主业务，返回数据库中的原始浏览量
            System.err.println("帖子浏览量计数异常: " + e.getMessage());
        }

        // 补充当前用户的点赞/收藏状态，供详情页高亮按钮
// 统计真实点赞数（详情页此前不返回 likeCount，前端取消点赞会出现 0-1=-1）
          LambdaQueryWrapper<LikeRecord> detailLikeWrapper = new LambdaQueryWrapper<>();
          detailLikeWrapper.eq(LikeRecord::getTargetType, "0");
          detailLikeWrapper.eq(LikeRecord::getTargetId, id);
          post.setLikeCount((int) likeRecordService.count(detailLikeWrapper));

        Long userId = UserContext.getUserId();
        if (userId != null) {
            LambdaQueryWrapper<LikeRecord> likeWrapper = new LambdaQueryWrapper<>();
            likeWrapper.eq(LikeRecord::getUserId, userId);
            likeWrapper.eq(LikeRecord::getTargetType, "0");
            likeWrapper.eq(LikeRecord::getTargetId, id);
            post.setIsLiked(likeRecordService.count(likeWrapper) > 0);

            LambdaQueryWrapper<Collect> collectWrapper = new LambdaQueryWrapper<>();
            collectWrapper.eq(Collect::getUserId, userId);
            collectWrapper.eq(Collect::getTargetType, TARGET_TYPE_POST);
            collectWrapper.eq(Collect::getTargetId, id);
            post.setIsCollected(collectService.count(collectWrapper) > 0);
        } else {
            post.setIsLiked(false);
            post.setIsCollected(false);
        }

        // 补充评论数（列表页已统计，详情页同样需要）
        LambdaQueryWrapper<Comment> commentCountWrapper = new LambdaQueryWrapper<>();
        commentCountWrapper.eq(Comment::getPostId, id);
        post.setCommentCount((int) commentService.count(commentCountWrapper));

        return Result.success(post);
    }

    /**
     * 发布帖子
     * 默认状态为审核通过（auditStatus = 1）
     * 学校ID从SchoolContext获取，用户ID从UserContext获取
     */
    @Operation(summary = "发布帖子")
    @PostMapping("/createPost")
    public Result<Post> createPost(@RequestBody Post post) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        Long schoolId = SchoolContext.getSchoolId();

        post.setUserId(userId);
        post.setSchoolId(schoolId);
        // 默认状态：审核通过
        post.setAuditStatus(1);
        // 浏览量初始为0
        post.setViewCount(0);
        // 分类：缺省「推荐」；带了分类但不在白名单内则明确报错，
        // 不做静默兜底 —— 否则用户明明选了「吐槽」，帖子却发到「推荐」，
        // 在所选分类里看不到自己的帖子，会误以为是功能失效。
        if (post.getCategory() == null || post.getCategory().trim().isEmpty()) {
            post.setCategory("推荐");
        } else if (!isValidCategory(post.getCategory())) {
            return Result.error("分类不合法，请重新选择（推荐/吐槽/交友/分享）");
        }

        postService.save(post);
        return Result.success(post);
    }

    /**
     * 校验帖子分类是否合法（推荐/吐槽/交友/分享）
     */
    private boolean isValidCategory(String category) {
        return "推荐".equals(category) || "吐槽".equals(category)
                || "交友".equals(category) || "分享".equals(category);
    }

    /**
     * 收藏 / 取消收藏帖子
     *
     * <p>前端帖子详情页的收藏按钮调用。已收藏则取消，未收藏则新增。
     * 与点赞逻辑一致，返回操作后的收藏状态。</p>
     */
    @Operation(summary = "收藏/取消收藏帖子")
    @PostMapping("/collectPost")
    public Result<Boolean> collectPost(HttpServletRequest request) {
        Long postId = ReqParams.requireLong(request, "postId");
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }

        Post post = postService.getById(postId);
        if (post == null) {
            return Result.error("帖子不存在");
        }

        LambdaQueryWrapper<Collect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Collect::getUserId, userId);
        wrapper.eq(Collect::getTargetType, TARGET_TYPE_POST);
        wrapper.eq(Collect::getTargetId, postId);
        Collect existing = collectService.getOne(wrapper);

        boolean collected;
        if (existing != null) {
            // 必须物理删除：collect 表上有 (user_id,target_id,target_type) 唯一索引，
            // 而 BaseEntity 的 deleted 字段启用了 MyBatis-Plus 逻辑删除，
            // removeById 只会把 deleted 置 1，唯一索引仍占用该行，
            // 导致「取消收藏后再收藏」报 Duplicate entry（数据保存失败）。
            collectMapper.deleteById(existing.getId());
            collected = false;
        } else {
            Collect collect = new Collect();
            collect.setUserId(userId);
            collect.setTargetType(TARGET_TYPE_POST);
            collect.setTargetId(postId);
            collectService.save(collect);
            collected = true;
        }
        return Result.success(collected);
    }

    /**
     * 我的收藏列表（支持按类型筛选）
     *
     * <p>targetType：0商品 1帖子 2活动；不传表示全部。</p>
     */
    @Operation(summary = "我的收藏列表")
    @GetMapping("/myCollects")
    public Result<List<Collect>> myCollects(@RequestParam(required = false) Integer targetType) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }

        LambdaQueryWrapper<Collect> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Collect::getUserId, userId);
        if (targetType != null) {
            wrapper.eq(Collect::getTargetType, targetType);
        }
        wrapper.orderByDesc(Collect::getCreateTime);
        List<Collect> list = collectService.list(wrapper);

        // 回填目标标题/封面，便于前端直接展示
        for (Collect c : list) {
            if (c.getTargetType() != null && c.getTargetType() == TARGET_TYPE_POST) {
                Post p = postService.getById(c.getTargetId());
                if (p != null) {
                    c.setTargetTitle(p.getTitle());
                    c.setTargetDesc(p.getContent());
                    if (p.getImages() != null && !p.getImages().isEmpty()) {
                        c.setTargetCover(p.getImages().split(",")[0]);
                    }
                }
            }
        }
        return Result.success(list);
    }

    /**
     * 评论列表
     * 根据帖子ID查询评论列表，分页返回，按创建时间倒序
     */
    @Operation(summary = "评论列表")
    @GetMapping("/commentList")
    public Result<Page<Comment>> commentList(PageQuery pageQuery,
                                             @RequestParam Long postId) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getPostId, postId);
        wrapper.orderByDesc(Comment::getCreateTime);

        Page<Comment> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        Page<Comment> result = commentService.page(page, wrapper);
        return Result.success(result);
    }

    /**
     * 发表评论
     * 用户ID从UserContext获取
     */
    @Operation(summary = "发表评论")
    @PostMapping("/createComment")
    public Result<Comment> createComment(@RequestBody Comment comment) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        comment.setUserId(userId);

        // comment.parent_id 有外键 fk_comment_parent → comment(id)，
        // 而该列默认值为 0，若前端传 0（或未传导致取默认值 0）会报外键冲突（HTTP 500）。
        // 这里统一把 0/负数视为「无父级」，置为 null。
        if (comment.getParentId() != null && comment.getParentId() <= 0) {
            comment.setParentId(null);
        }
        // 同理，回复目标用户为 0 也会触发 fk_comment_reply_user 外键冲突
        if (comment.getReplyUserId() != null && comment.getReplyUserId() <= 0) {
            comment.setReplyUserId(null);
        }

        commentService.save(comment);
        return Result.success(comment);
    }

    /**
     * 点赞/取消点赞帖子
     * 使用 LikeRecord 的 targetType 字段区分目标类型：0=帖子，1=评论
     * 根据当前用户是否已有点赞记录进行切换
     */
    @Operation(summary = "点赞/取消点赞帖子")
    @PostMapping("/likePost")
    public Result<Boolean> likePost(HttpServletRequest request) {
        Long postId = ReqParams.requireLong(request, "postId");
        Long userId = UserContext.getUserId();

        // 检查是否已点赞（targetType = "0" 表示帖子）
        LambdaQueryWrapper<LikeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LikeRecord::getTargetId, postId);
        wrapper.eq(LikeRecord::getTargetType, "0");
        wrapper.eq(LikeRecord::getUserId, userId);
        LikeRecord existing = likeRecordService.getOne(wrapper);

        // 检查帖子是否存在
        Post post = postService.getById(postId);
        if (post == null) {
            return Result.error("帖子不存在");
        }

        boolean liked;
        if (existing != null) {
            // 已点赞，取消点赞
            likeRecordService.removeById(existing.getId());
            liked = false;
        } else {
            // 未点赞，执行点赞
            LikeRecord like = new LikeRecord();
            like.setTargetId(postId);
            like.setTargetType("0"); // 0 表示帖子
            like.setUserId(userId);
            likeRecordService.save(like);
            liked = true;
        }

        return Result.success(liked);
    }

    /**
     * 我的帖子
     * 查询当前用户发布的所有帖子，按创建时间倒序
     */
    @Operation(summary = "我的帖子")
    @GetMapping("/myPosts")
    public Result<Page<Post>> myPosts(PageQuery pageQuery) {
        Long userId = UserContext.getUserId();
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getUserId, userId);
        wrapper.orderByDesc(Post::getCreateTime);

        Page<Post> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        Page<Post> result = postService.page(page, wrapper);
        return Result.success(result);
    }
}
