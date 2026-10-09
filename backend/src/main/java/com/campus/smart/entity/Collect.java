package com.campus.smart.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户收藏
 *
 * <p>支持收藏商品 / 帖子 / 活动三类内容（targetType 区分）。
 * 与 like_record（点赞）分开存储，避免语义混淆。</p>
 *
 * <p><b>注意：本表不做逻辑删除（无 deleted 字段）。</b>
 * 因为表上有 (user_id, target_id, target_type) 唯一索引，
 * 若启用逻辑删除，取消收藏只是把 deleted 置 1，唯一索引仍占用该行，
 * 导致「取消收藏后再收藏」触发 Duplicate entry。
 * 因此取消收藏使用物理删除（deleteById），查询也不带 deleted 条件。</p>
 */
@Data
@TableName(value = "collect")
public class Collect implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 目标ID（商品/帖子/活动） */
    private Long targetId;

    /** 目标类型：0商品 1帖子 2活动 */
    private Integer targetType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 目标标题（非数据库字段，列表展示时填充） */
    @TableField(exist = false)
    private String targetTitle;

    /** 目标封面图（非数据库字段） */
    @TableField(exist = false)
    private String targetCover;

    /** 目标摘要（非数据库字段） */
    @TableField(exist = false)
    private String targetDesc;
}
