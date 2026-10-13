package com.campus.smart.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 问答对话记录
 *
 * @author 万盛博
 */
@Data
@TableName("ai_chat_record")
public class AiChatRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 用户问题 */
    private String question;

    /** AI 回答 */
    private String answer;

    /** 本次召回的知识片段ID列表（逗号分隔） */
    private String retrievedIds;

    /** 是否命中缓存 0:否 1:是 */
    private Integer hitCache;

    /** 响应耗时（毫秒） */
    private Integer durationMs;

    /** 使用的模型 */
    private String model;

    /** 状态 0:失败 1:成功 */
    private Integer status;

    private LocalDateTime createTime;
}
