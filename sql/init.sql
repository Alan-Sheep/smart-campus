-- ============================
-- 智慧校园小程序 - 数据库建表脚本
-- 数据库: MySQL 8.0
-- 字符集: utf8mb4
-- ============================

CREATE DATABASE IF NOT EXISTS smart_campus DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE smart_campus;

-- ----------------------------
-- 用户表
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `openid`      VARCHAR(64)  DEFAULT NULL COMMENT '微信openid',
  `nickname`    VARCHAR(64)  NOT NULL COMMENT '昵称',
  `avatar`      VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
  `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除(0:正常 1:删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid` (`openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ----------------------------
-- 商品分类表
-- ----------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `name`        VARCHAR(32)  NOT NULL COMMENT '分类名称',
  `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- ----------------------------
-- 二手商品表
-- ----------------------------
DROP TABLE IF EXISTS `goods`;
CREATE TABLE `goods` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT       NOT NULL COMMENT '发布者ID',
  `title`       VARCHAR(128) NOT NULL COMMENT '商品标题',
  `description` TEXT         DEFAULT NULL COMMENT '商品描述',
  `price`       DECIMAL(10,2) NOT NULL COMMENT '价格',
  `images`      VARCHAR(2048) DEFAULT NULL COMMENT '图片URL(逗号分隔)',
  `category_id` BIGINT       DEFAULT NULL COMMENT '分类ID',
  `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态(0:在售 1:已售 2:下架)',
  `view_count`  INT          NOT NULL DEFAULT 0 COMMENT '浏览量',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='二手商品表';

-- ----------------------------
-- 帖子表
-- ----------------------------
DROP TABLE IF EXISTS `post`;
CREATE TABLE `post` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT       NOT NULL COMMENT '作者ID',
  `title`       VARCHAR(128) NOT NULL COMMENT '标题',
  `content`     TEXT         NOT NULL COMMENT '内容',
  `images`      VARCHAR(2048) DEFAULT NULL COMMENT '图片URL(逗号分隔)',
  `view_count`  INT          NOT NULL DEFAULT 0 COMMENT '浏览量',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子表';

-- ----------------------------
-- 评论表
-- ----------------------------
DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `post_id`     BIGINT       NOT NULL COMMENT '帖子ID',
  `user_id`     BIGINT       NOT NULL COMMENT '评论者ID',
  `content`     VARCHAR(512) NOT NULL COMMENT '评论内容',
  `parent_id`   BIGINT       DEFAULT 0 COMMENT '父评论ID(0:顶级评论)',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- ----------------------------
-- 点赞记录表
-- ----------------------------
DROP TABLE IF EXISTS `like_record`;
CREATE TABLE `like_record` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `target_id`   BIGINT       NOT NULL COMMENT '目标ID(帖子ID/评论ID)',
  `target_type` TINYINT      NOT NULL COMMENT '目标类型(0:帖子 1:评论)',
  `user_id`     BIGINT       NOT NULL COMMENT '点赞用户ID',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_target_user` (`target_id`, `target_type`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞记录表';

-- ----------------------------
-- 失物招领表
-- ----------------------------
DROP TABLE IF EXISTS `lost_found`;
CREATE TABLE `lost_found` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT       NOT NULL COMMENT '发布者ID',
  `type`        TINYINT      NOT NULL COMMENT '类型(0:寻物 1:招领)',
  `item_name`   VARCHAR(128) NOT NULL COMMENT '物品名称',
  `description` TEXT         DEFAULT NULL COMMENT '物品描述',
  `images`      VARCHAR(2048) DEFAULT NULL COMMENT '图片URL(逗号分隔)',
  `location`    VARCHAR(256) DEFAULT NULL COMMENT '地点',
  `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态(0:未解决 1:已解决)',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_type` (`type`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='失物招领表';

-- ----------------------------
-- 操作日志表
-- ----------------------------
DROP TABLE IF EXISTS `oper_log`;
CREATE TABLE `oper_log` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT       DEFAULT NULL COMMENT '操作用户ID',
  `operation`   VARCHAR(64)  DEFAULT NULL COMMENT '操作描述',
  `method`      VARCHAR(256) DEFAULT NULL COMMENT '请求方法',
  `params`      TEXT         DEFAULT NULL COMMENT '请求参数',
  `cost_time`   BIGINT       DEFAULT NULL COMMENT '耗时(ms)',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ----------------------------
-- 初始分类数据
-- ----------------------------
INSERT INTO `category` (`name`, `sort`) VALUES
  ('数码电子', 1),
  ('书籍教材', 2),
  ('生活用品', 3),
  ('服装鞋包', 4),
  ('运动器材', 5),
  ('其他', 99);
