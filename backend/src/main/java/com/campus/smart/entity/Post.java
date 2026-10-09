package com.campus.smart.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("post")
public class Post extends BaseEntity {

    private Long schoolId;

    private Long userId;

    private String title;

    private String content;

    private String images;

    /**
     * 分类：推荐 / 吐槽 / 交友 / 分享
     *
     * <p>校园频道顶部有分类标签，原先数据库无该字段，导致点分类只能重新加载全部帖子。</p>
     */
    private String category;

    private Integer viewCount;

    private Integer auditStatus;

    private Integer isTop;

    private String rejectReason;

    /**
     * 点赞数（非数据库字段，列表查询时统计）
     */
    @TableField(exist = false)
    private Integer likeCount;

    /**
     * 评论数（非数据库字段，列表查询时统计）
     */
    @TableField(exist = false)
    private Integer commentCount;

    /**
     * 当前用户是否已点赞（非数据库字段）
     */
    @TableField(exist = false)
    private Boolean isLiked;

    /**
     * 当前用户是否已收藏（非数据库字段）
     */
    @TableField(exist = false)
    private Boolean isCollected;
}
