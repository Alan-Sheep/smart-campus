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

@Data
@TableName("comment")
public class Comment implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;

    private Long userId;

    private String content;

    /**
     * 父评论ID（顶级评论为 NULL）
     *
     * <p>必须用 {@code FieldStrategy.IGNORED}（本版本未提供 ALWAYS）：该列在库中默认值为 0，
     * 但存在外键 {@code fk_comment_parent → comment(id)}，并不存在 id=0 的行。
     * MyBatis-Plus 默认 NOT_NULL 策略会把 null 字段从 INSERT 中省略，
     * 于是数据库套用默认值 0 → 触发外键冲突报 500。
     * 声明 IGNORED 后该字段始终参与 SQL，null 会写成真正的 {@code NULL}，绕开默认值。</p>
     */
    @TableField(insertStrategy = FieldStrategy.IGNORED, updateStrategy = FieldStrategy.IGNORED)
    private Long parentId;

    /**
     * 被回复用户ID（无则为 NULL），同样需要 IGNORED 以避开默认值 0 导致的外键冲突
     */
    @TableField(insertStrategy = FieldStrategy.IGNORED, updateStrategy = FieldStrategy.IGNORED)
    private Long replyUserId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
