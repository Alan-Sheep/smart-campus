-- v8: AI 智能问答模块（RAG 知识库 + 对话记录）
USE smart_campus;

-- 知识片段表：校园公告/规则切块后的向量化存储
CREATE TABLE IF NOT EXISTS ai_knowledge (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '片段ID',
  source_type TINYINT NOT NULL DEFAULT 0 COMMENT '来源类型 0:公告 1:帖子 2:业务规则 3:手动录入',
  source_id BIGINT NULL COMMENT '来源记录ID（如 notice.id）',
  title VARCHAR(128) NULL COMMENT '来源标题',
  content TEXT NOT NULL COMMENT '切分后的文本片段',
  embedding_json MEDIUMTEXT NULL COMMENT '向量(JSON数组，1536维)',
  embedding_model VARCHAR(64) NULL COMMENT 'Embedding 模型标识',
  token_count INT NULL COMMENT '片段token估算数',
  enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用 0:否 1:是',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_source (source_type, source_id),
  KEY idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI知识库片段（RAG检索用）';

-- 对话记录表：AI 问答历史
CREATE TABLE IF NOT EXISTS ai_chat_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  question VARCHAR(512) NOT NULL COMMENT '用户问题',
  answer TEXT NULL COMMENT 'AI回答',
  retrieved_ids VARCHAR(512) NULL COMMENT '本次召回的知识片段ID列表(逗号分隔)',
  hit_cache TINYINT NOT NULL DEFAULT 0 COMMENT '是否命中缓存 0:否 1:是',
  duration_ms INT NULL COMMENT '响应耗时(毫秒)',
  model VARCHAR(64) NULL COMMENT '使用的模型',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0:失败 1:成功',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI问答对话记录';

SHOW TABLES LIKE 'ai_%';