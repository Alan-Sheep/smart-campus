package com.campus.smart.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 知识库片段（RAG 检索用）
 * 校园公告/业务规则等文档切块后的向量化存储
 *
 * @author 万盛博
 */
@Data
@TableName("ai_knowledge")
public class AiKnowledge {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 来源类型 0:公告 1:帖子 2:业务规则 3:手动录入 */
    private Integer sourceType;

    /** 来源记录ID（如 notice.id） */
    private Long sourceId;

    /** 来源标题 */
    private String title;

    /** 切分后的文本片段 */
    private String content;

    /**
     * 向量（JSON 数组）
     * 采用字符 n-gram hash 的本地轻量向量化方案：
     * 数据量小（<1万段）时无需调用远程 Embedding API，检索在内存中完成，
     * 避免引入向量数据库的过度设计（可按数据规模平滑升级为专业 Embedding + 向量库）
     */
    private String embeddingJson;

    /** 向量化模型标识 */
    private String embeddingModel;

    /** 片段 token 估算数 */
    private Integer tokenCount;

    /** 是否启用 0:否 1:是 */
    private Integer enabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
