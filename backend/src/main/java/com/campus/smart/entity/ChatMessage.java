package com.campus.smart.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("chat_message")
public class ChatMessage implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String conversationId;

    private Long fromUserId;

    private Long toUserId;

    /**
     * 消息类型（库列为 TINYINT：0:文本 1:图片 2:系统）
     *
     * <p>原先声明为 String，而前端传 'text'，MySQL 严格模式报
     * {@code Incorrect integer value}（HTTP 500）。改为 Integer 与列类型一致。</p>
     */
    private Integer msgType;

    private String content;

    private Integer isRead;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
