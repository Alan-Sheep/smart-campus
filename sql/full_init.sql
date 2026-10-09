-- ============================
-- 智慧校园小程序 - 完整数据库初始化脚本
-- 数据库: MySQL 8.0
-- 字符集: utf8mb4
-- 包含: 建库 + 建表 + 测试数据
-- ============================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 建库
-- ----------------------------
CREATE DATABASE IF NOT EXISTS smart_campus DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE smart_campus;

-- ============================
-- 一、表结构
-- ============================

-- ----------------------------
-- 1. 学校表
-- ----------------------------
DROP TABLE IF EXISTS `school`;
CREATE TABLE `school` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name`        VARCHAR(128) NOT NULL COMMENT '学校名称',
  `logo`        VARCHAR(512) DEFAULT NULL COMMENT '学校logo',
  `province`    VARCHAR(32)  DEFAULT NULL COMMENT '省份',
  `city`        VARCHAR(32)  DEFAULT NULL COMMENT '城市',
  `address`     VARCHAR(256) DEFAULT NULL COMMENT '详细地址',
  `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态(0:禁用 1:启用)',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学校表';

-- ----------------------------
-- 2. 管理员表
-- ----------------------------
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username`    VARCHAR(64)  NOT NULL COMMENT '用户名',
  `password`    VARCHAR(128) NOT NULL COMMENT '密码(加密)',
  `nickname`    VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
  `avatar`      VARCHAR(512) DEFAULT NULL COMMENT '头像',
  `school_id`   BIGINT       DEFAULT NULL COMMENT '学校ID(空=超管)',
  `role`        TINYINT      NOT NULL DEFAULT 1 COMMENT '角色(1:普通管理员 2:超级管理员)',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态(0:禁用 1:正常)',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_school_id` (`school_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

-- ----------------------------
-- 3. 用户表
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `openid`      VARCHAR(64)  DEFAULT NULL COMMENT '微信openid',
  `nickname`    VARCHAR(64)  NOT NULL COMMENT '昵称',
  `avatar`      VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
  `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
  `school_id`   BIGINT       DEFAULT NULL COMMENT '学校ID',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态(0:禁用 1:正常)',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除(0:正常 1:删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid` (`openid`),
  KEY `idx_school_id` (`school_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ----------------------------
-- 4. 轮播图表
-- ----------------------------
DROP TABLE IF EXISTS `banner`;
CREATE TABLE `banner` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `school_id`   BIGINT       DEFAULT 0 COMMENT '学校ID(0=全局)',
  `title`       VARCHAR(128) NOT NULL COMMENT '标题',
  `image`       VARCHAR(512) NOT NULL COMMENT '图片URL',
  `link_type`   TINYINT      NOT NULL DEFAULT 0 COMMENT '跳转类型(0:无 1:内部页面 2:外部链接)',
  `link_url`    VARCHAR(512) DEFAULT NULL COMMENT '跳转地址',
  `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态(0:禁用 1:启用)',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_school_id` (`school_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='轮播图表';

-- ----------------------------
-- 5. 公告表
-- ----------------------------
DROP TABLE IF EXISTS `notice`;
CREATE TABLE `notice` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `school_id`   BIGINT       DEFAULT 0 COMMENT '学校ID(0=全局)',
  `title`       VARCHAR(128) NOT NULL COMMENT '标题',
  `content`     VARCHAR(512) NOT NULL COMMENT '内容',
  `is_top`      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否置顶(0:否 1:是)',
  `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态(0:禁用 1:启用)',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_school_id` (`school_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

-- ----------------------------
-- 6. 商品分类表
-- ----------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `school_id`   BIGINT       DEFAULT 0 COMMENT '学校ID(0表示全局)',
  `name`        VARCHAR(32)  NOT NULL COMMENT '分类名称',
  `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_school_id` (`school_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- ----------------------------
-- 7. 二手商品表
-- ----------------------------
DROP TABLE IF EXISTS `goods`;
CREATE TABLE `goods` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT       NOT NULL COMMENT '发布者ID',
  `school_id`   BIGINT       DEFAULT NULL COMMENT '学校ID',
  `title`       VARCHAR(128) NOT NULL COMMENT '商品标题',
  `description` TEXT         DEFAULT NULL COMMENT '商品描述',
  `price`       DECIMAL(10,2) NOT NULL COMMENT '价格',
  `images`      VARCHAR(2048) DEFAULT NULL COMMENT '图片URL(逗号分隔)',
  `category_id` BIGINT       DEFAULT NULL COMMENT '分类ID',
  `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态(0:在售 1:已售 2:下架)',
  `audit_status` TINYINT     NOT NULL DEFAULT 1 COMMENT '审核状态(0:待审核 1:已通过 2:已驳回)',
  `reject_reason` VARCHAR(256) DEFAULT NULL COMMENT '驳回原因',
  `view_count`  INT          NOT NULL DEFAULT 0 COMMENT '浏览量',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_school_id` (`school_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status` (`status`),
  KEY `idx_audit_status` (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='二手商品表';

-- ----------------------------
-- 8. 帖子表
-- ----------------------------
DROP TABLE IF EXISTS `post`;
CREATE TABLE `post` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT       NOT NULL COMMENT '作者ID',
  `school_id`   BIGINT       DEFAULT NULL COMMENT '学校ID',
  `title`       VARCHAR(128) NOT NULL COMMENT '标题',
  `content`     TEXT         NOT NULL COMMENT '内容',
  `images`      VARCHAR(2048) DEFAULT NULL COMMENT '图片URL(逗号分隔)',
  `view_count`  INT          NOT NULL DEFAULT 0 COMMENT '浏览量',
  `audit_status` TINYINT     NOT NULL DEFAULT 1 COMMENT '审核状态(0:待审核 1:已通过 2:已驳回)',
  `is_top`      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否置顶(0:否 1:是)',
  `reject_reason` VARCHAR(256) DEFAULT NULL COMMENT '驳回原因',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_school_id` (`school_id`),
  KEY `idx_audit_status` (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子表';

-- ----------------------------
-- 9. 评论表
-- ----------------------------
DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `post_id`     BIGINT       NOT NULL COMMENT '帖子ID',
  `user_id`     BIGINT       NOT NULL COMMENT '评论者ID',
  `content`     VARCHAR(512) NOT NULL COMMENT '评论内容',
  `parent_id`   BIGINT       DEFAULT 0 COMMENT '父评论ID(0:顶级评论)',
  `reply_user_id` BIGINT     DEFAULT NULL COMMENT '被回复用户ID',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- ----------------------------
-- 10. 点赞记录表
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
-- 11. 失物招领表
-- ----------------------------
DROP TABLE IF EXISTS `lost_found`;
CREATE TABLE `lost_found` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT       NOT NULL COMMENT '发布者ID',
  `school_id`   BIGINT       DEFAULT NULL COMMENT '学校ID',
  `type`        TINYINT      NOT NULL COMMENT '类型(0:寻物 1:招领)',
  `item_name`   VARCHAR(128) NOT NULL COMMENT '物品名称',
  `description` TEXT         DEFAULT NULL COMMENT '物品描述',
  `images`      VARCHAR(2048) DEFAULT NULL COMMENT '图片URL(逗号分隔)',
  `location`    VARCHAR(256) DEFAULT NULL COMMENT '地点',
  `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态(0:未解决 1:已解决)',
  `audit_status` TINYINT     NOT NULL DEFAULT 1 COMMENT '审核状态(0:待审核 1:已通过 2:已驳回)',
  `reject_reason` VARCHAR(256) DEFAULT NULL COMMENT '驳回原因',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_type` (`type`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_school_id` (`school_id`),
  KEY `idx_audit_status` (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='失物招领表';

-- ----------------------------
-- 12. 快递代取订单表
-- ----------------------------
DROP TABLE IF EXISTS `express_order`;
CREATE TABLE `express_order` (
  `id`              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no`        VARCHAR(32)    NOT NULL COMMENT '订单号',
  `school_id`       BIGINT         NOT NULL COMMENT '学校ID',
  `user_id`         BIGINT         NOT NULL COMMENT '下单用户ID',
  `picker_id`       BIGINT         DEFAULT NULL COMMENT '接单用户ID',
  `express_company` VARCHAR(32)    NOT NULL COMMENT '快递公司',
  `pickup_code`     VARCHAR(32)    NOT NULL COMMENT '取件码',
  `pickup_address`  VARCHAR(256)   NOT NULL COMMENT '取件地址',
  `delivery_address` VARCHAR(256)  NOT NULL COMMENT '送达地址',
  `weight`          TINYINT        DEFAULT 0 COMMENT '重量(0:小件 1:中件 2:大件)',
  `receiver_name`   VARCHAR(32)    NOT NULL COMMENT '收货人姓名',
  `receiver_phone`  VARCHAR(20)    NOT NULL COMMENT '收货人电话',
  `remark`          VARCHAR(512)   DEFAULT NULL COMMENT '备注',
  `service_fee`     DECIMAL(10,2)  NOT NULL COMMENT '服务费',
  `tip`             DECIMAL(10,2)  DEFAULT 0 COMMENT '小费',
  `total_amount`    DECIMAL(10,2)  NOT NULL COMMENT '总金额',
  `status`          TINYINT        NOT NULL DEFAULT 0 COMMENT '订单状态(0:待接单 1:已接单 2:配送中 3:已完成 4:已取消 5:已退款)',
  `cancel_reason`   VARCHAR(256)   DEFAULT NULL COMMENT '取消原因',
  `accept_time`     DATETIME       DEFAULT NULL COMMENT '接单时间',
  `complete_time`   DATETIME       DEFAULT NULL COMMENT '完成时间',
  `create_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_school_id` (`school_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='快递代取订单表';

-- ----------------------------
-- 13. 打印服务订单表
-- ----------------------------
DROP TABLE IF EXISTS `print_order`;
CREATE TABLE `print_order` (
  `id`              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no`        VARCHAR(32)    NOT NULL COMMENT '订单号',
  `school_id`       BIGINT         NOT NULL COMMENT '学校ID',
  `user_id`         BIGINT         NOT NULL COMMENT '下单用户ID',
  `printer_id`      BIGINT         DEFAULT NULL COMMENT '接单用户ID',
  `file_urls`       VARCHAR(2048)  NOT NULL COMMENT '文件URL(逗号分隔)',
  `print_type`      TINYINT        NOT NULL DEFAULT 0 COMMENT '打印类型(0:黑白 1:彩色)',
  `paper_size`      VARCHAR(16)    NOT NULL DEFAULT 'A4' COMMENT '纸张尺寸',
  `copies`          INT            NOT NULL DEFAULT 1 COMMENT '份数',
  `is_double_side`  TINYINT        NOT NULL DEFAULT 0 COMMENT '是否双面(0:否 1:是)',
  `binding`         TINYINT        NOT NULL DEFAULT 0 COMMENT '装订方式(0:无 1:订书钉 2:胶装)',
  `delivery_address` VARCHAR(256)  NOT NULL COMMENT '送达地址',
  `receiver_name`   VARCHAR(32)    NOT NULL COMMENT '收货人姓名',
  `receiver_phone`  VARCHAR(20)    NOT NULL COMMENT '收货人电话',
  `remark`          VARCHAR(512)   DEFAULT NULL COMMENT '备注',
  `total_pages`     INT            DEFAULT 0 COMMENT '总页数',
  `total_amount`    DECIMAL(10,2)  NOT NULL COMMENT '总金额',
  `status`          TINYINT        NOT NULL DEFAULT 0 COMMENT '订单状态(0:待接单 1:已接单 2:打印中 3:已完成 4:已取消 5:已退款)',
  `cancel_reason`   VARCHAR(256)   DEFAULT NULL COMMENT '取消原因',
  `accept_time`     DATETIME       DEFAULT NULL COMMENT '接单时间',
  `complete_time`   DATETIME       DEFAULT NULL COMMENT '完成时间',
  `create_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_school_id` (`school_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打印服务订单表';

-- ----------------------------
-- 14. 校园跑腿订单表
-- ----------------------------
DROP TABLE IF EXISTS `errand_order`;
CREATE TABLE `errand_order` (
  `id`              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no`        VARCHAR(32)    NOT NULL COMMENT '订单号',
  `school_id`       BIGINT         NOT NULL COMMENT '学校ID',
  `user_id`         BIGINT         NOT NULL COMMENT '下单用户ID',
  `runner_id`       BIGINT         DEFAULT NULL COMMENT '接单用户ID',
  `errand_type`     TINYINT        NOT NULL DEFAULT 0 COMMENT '跑腿类型(0:帮我买 1:帮我送 2:帮我取 3:其他)',
  `title`           VARCHAR(128)   NOT NULL COMMENT '标题',
  `description`     TEXT           DEFAULT NULL COMMENT '详细描述',
  `images`          VARCHAR(2048)  DEFAULT NULL COMMENT '图片(逗号分隔)',
  `pickup_address`  VARCHAR(256)   DEFAULT NULL COMMENT '取货地址',
  `delivery_address` VARCHAR(256)  NOT NULL COMMENT '送达地址',
  `receiver_name`   VARCHAR(32)    NOT NULL COMMENT '收货人姓名',
  `receiver_phone`  VARCHAR(20)    NOT NULL COMMENT '收货人电话',
  `estimated_time`  DATETIME       DEFAULT NULL COMMENT '期望送达时间',
  `service_fee`     DECIMAL(10,2)  NOT NULL COMMENT '跑腿费',
  `goods_fee`       DECIMAL(10,2)  DEFAULT 0 COMMENT '商品费用(代买时)',
  `tip`             DECIMAL(10,2)  DEFAULT 0 COMMENT '小费',
  `total_amount`    DECIMAL(10,2)  NOT NULL COMMENT '总金额',
  `status`          TINYINT        NOT NULL DEFAULT 0 COMMENT '订单状态(0:待接单 1:已接单 2:进行中 3:已完成 4:已取消 5:已退款)',
  `cancel_reason`   VARCHAR(256)   DEFAULT NULL COMMENT '取消原因',
  `accept_time`     DATETIME       DEFAULT NULL COMMENT '接单时间',
  `complete_time`   DATETIME       DEFAULT NULL COMMENT '完成时间',
  `create_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_school_id` (`school_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='校园跑腿订单表';

-- ----------------------------
-- 15. 代课服务订单表
-- ----------------------------
DROP TABLE IF EXISTS `substitute_order`;
CREATE TABLE `substitute_order` (
  `id`              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no`        VARCHAR(32)    NOT NULL COMMENT '订单号',
  `school_id`       BIGINT         NOT NULL COMMENT '学校ID',
  `user_id`         BIGINT         NOT NULL COMMENT '下单用户ID',
  `substitute_id`   BIGINT         DEFAULT NULL COMMENT '代课用户ID',
  `course_name`     VARCHAR(128)   NOT NULL COMMENT '课程名称',
  `course_time`     VARCHAR(64)    NOT NULL COMMENT '上课时间(如:周一第1-2节)',
  `course_date`     DATE           NOT NULL COMMENT '上课日期',
  `classroom`       VARCHAR(64)    NOT NULL COMMENT '教室',
  `teacher`         VARCHAR(32)    DEFAULT NULL COMMENT '老师姓名',
  `major`           VARCHAR(64)    DEFAULT NULL COMMENT '专业',
  `requirement`     VARCHAR(512)   DEFAULT NULL COMMENT '要求',
  `reward`          DECIMAL(10,2)  NOT NULL COMMENT '报酬',
  `contact_qq`      VARCHAR(32)    DEFAULT NULL COMMENT '联系QQ',
  `contact_wechat`  VARCHAR(32)    DEFAULT NULL COMMENT '联系微信',
  `status`          TINYINT        NOT NULL DEFAULT 0 COMMENT '订单状态(0:待接单 1:已接单 2:已完成 3:已取消 4:已退款)',
  `cancel_reason`   VARCHAR(256)   DEFAULT NULL COMMENT '取消原因',
  `accept_time`     DATETIME       DEFAULT NULL COMMENT '接单时间',
  `complete_time`   DATETIME       DEFAULT NULL COMMENT '完成时间',
  `create_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_school_id` (`school_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代课服务订单表';

-- ----------------------------
-- 16. 校园活动表
-- ----------------------------
DROP TABLE IF EXISTS `activity`;
CREATE TABLE `activity` (
  `id`              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `school_id`       BIGINT         NOT NULL COMMENT '学校ID',
  `user_id`         BIGINT         NOT NULL COMMENT '发布者ID',
  `title`           VARCHAR(128)   NOT NULL COMMENT '活动标题',
  `description`     TEXT           DEFAULT NULL COMMENT '活动描述',
  `images`          VARCHAR(2048)  DEFAULT NULL COMMENT '活动图片(逗号分隔)',
  `activity_type`   TINYINT        NOT NULL DEFAULT 0 COMMENT '活动类型(0:学习 1:运动 2:娱乐 3:志愿 4:其他)',
  `activity_date`   DATE           NOT NULL COMMENT '活动日期',
  `start_time`      TIME           NOT NULL COMMENT '开始时间',
  `end_time`        TIME           NOT NULL COMMENT '结束时间',
  `location`        VARCHAR(256)   NOT NULL COMMENT '活动地点',
  `max_participants` INT           DEFAULT 0 COMMENT '最大人数(0=不限)',
  `current_participants` INT       NOT NULL DEFAULT 0 COMMENT '当前报名人数',
  `fee`             DECIMAL(10,2)  DEFAULT 0 COMMENT '费用',
  `contact_way`     VARCHAR(128)   DEFAULT NULL COMMENT '联系方式',
  `audit_status`    TINYINT        NOT NULL DEFAULT 1 COMMENT '审核状态(0:待审核 1:已通过 2:已驳回)',
  `reject_reason`   VARCHAR(256)   DEFAULT NULL COMMENT '驳回原因',
  `status`          TINYINT        NOT NULL DEFAULT 1 COMMENT '状态(0:已结束 1:报名中 2:进行中)',
  `view_count`      INT            NOT NULL DEFAULT 0 COMMENT '浏览量',
  `is_top`          TINYINT        NOT NULL DEFAULT 0 COMMENT '是否置顶(0:否 1:是)',
  `create_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_school_id` (`school_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_audit_status` (`audit_status`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='校园活动表';

-- ----------------------------
-- 17. 活动报名表
-- ----------------------------
DROP TABLE IF EXISTS `activity_signup`;
CREATE TABLE `activity_signup` (
  `id`              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `activity_id`     BIGINT         NOT NULL COMMENT '活动ID',
  `user_id`         BIGINT         NOT NULL COMMENT '报名用户ID',
  `name`            VARCHAR(32)    DEFAULT NULL COMMENT '姓名',
  `phone`           VARCHAR(20)    DEFAULT NULL COMMENT '手机号',
  `remark`          VARCHAR(256)   DEFAULT NULL COMMENT '备注',
  `status`          TINYINT        NOT NULL DEFAULT 1 COMMENT '状态(0:取消 1:已报名 2:已参加)',
  `create_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_activity_user` (`activity_id`, `user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动报名表';

-- ----------------------------
-- 18. 游戏搭子表
-- ----------------------------
DROP TABLE IF EXISTS `game_buddy`;
CREATE TABLE `game_buddy` (
  `id`              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `school_id`       BIGINT         NOT NULL COMMENT '学校ID',
  `user_id`         BIGINT         NOT NULL COMMENT '发布者ID',
  `game_name`       VARCHAR(64)    NOT NULL COMMENT '游戏名称',
  `title`           VARCHAR(128)   NOT NULL COMMENT '标题',
  `description`     VARCHAR(512)   DEFAULT NULL COMMENT '描述',
  `game_level`      VARCHAR(32)    DEFAULT NULL COMMENT '游戏段位/等级',
  `play_mode`       VARCHAR(32)    DEFAULT NULL COMMENT '玩法模式',
  `contact_way`     VARCHAR(128)   DEFAULT NULL COMMENT '联系方式',
  `play_time`       VARCHAR(64)    DEFAULT NULL COMMENT '可玩时间',
  `gender`          TINYINT        DEFAULT 2 COMMENT '性别要求(0:男 1:女 2:不限)',
  `status`          TINYINT        NOT NULL DEFAULT 1 COMMENT '状态(0:已结束 1:招募中)',
  `view_count`      INT            NOT NULL DEFAULT 0 COMMENT '浏览量',
  `create_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_school_id` (`school_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_game_name` (`game_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游戏搭子表';

-- ----------------------------
-- 19. 系统消息表
-- ----------------------------
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
  `id`              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`         BIGINT         NOT NULL COMMENT '接收用户ID',
  `type`            TINYINT        NOT NULL COMMENT '消息类型(0:系统通知 1:订单消息 2:互动消息)',
  `title`           VARCHAR(128)   NOT NULL COMMENT '消息标题',
  `content`         VARCHAR(1024)  NOT NULL COMMENT '消息内容',
  `related_id`      BIGINT         DEFAULT NULL COMMENT '关联ID(订单ID/帖子ID等)',
  `related_type`    VARCHAR(32)    DEFAULT NULL COMMENT '关联类型',
  `is_read`         TINYINT        NOT NULL DEFAULT 0 COMMENT '是否已读(0:未读 1:已读)',
  `create_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_is_read` (`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统消息表';

-- ----------------------------
-- 20. 聊天消息表
-- ----------------------------
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message` (
  `id`              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `conversation_id` VARCHAR(64)    NOT NULL COMMENT '会话ID(用户1ID_用户2ID)',
  `from_user_id`    BIGINT         NOT NULL COMMENT '发送者ID',
  `to_user_id`      BIGINT         NOT NULL COMMENT '接收者ID',
  `msg_type`        TINYINT        NOT NULL DEFAULT 0 COMMENT '消息类型(0:文本 1:图片 2:系统)',
  `content`         VARCHAR(2048)  NOT NULL COMMENT '消息内容',
  `is_read`         TINYINT        NOT NULL DEFAULT 0 COMMENT '是否已读(0:未读 1:已读)',
  `create_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_conversation_id` (`conversation_id`),
  KEY `idx_from_user` (`from_user_id`),
  KEY `idx_to_user` (`to_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表';

-- ----------------------------
-- 21. 用户反馈表
-- ----------------------------
DROP TABLE IF EXISTS `feedback`;
CREATE TABLE `feedback` (
  `id`              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `school_id`       BIGINT         DEFAULT NULL COMMENT '学校ID',
  `user_id`         BIGINT         NOT NULL COMMENT '反馈用户ID',
  `type`            TINYINT        NOT NULL DEFAULT 0 COMMENT '反馈类型(0:功能建议 1:BUG反馈 2:投诉举报 3:其他)',
  `title`           VARCHAR(128)   NOT NULL COMMENT '标题',
  `content`         TEXT           NOT NULL COMMENT '反馈内容',
  `images`          VARCHAR(2048)  DEFAULT NULL COMMENT '图片(逗号分隔)',
  `contact`         VARCHAR(64)    DEFAULT NULL COMMENT '联系方式',
  `status`          TINYINT        NOT NULL DEFAULT 0 COMMENT '状态(0:待处理 1:处理中 2:已完成)',
  `reply`           TEXT           DEFAULT NULL COMMENT '回复内容',
  `reply_time`      DATETIME       DEFAULT NULL COMMENT '回复时间',
  `reply_admin_id`  BIGINT         DEFAULT NULL COMMENT '回复管理员ID',
  `create_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_school_id` (`school_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户反馈表';

-- ----------------------------
-- 22. 操作日志表
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

-- ============================
-- 二、测试数据
-- ============================

-- ----------------------------
-- 1. 学校表测试数据
-- ----------------------------
INSERT INTO `school` (`id`, `name`, `logo`, `province`, `city`, `address`, `sort`, `status`) VALUES
(1, '北京大学', 'https://picsum.photos/200/200?random=101', '北京市', '北京市', '海淀区颐和园路5号', 1, 1),
(2, '清华大学', 'https://picsum.photos/200/200?random=102', '北京市', '北京市', '海淀区双清路30号', 2, 1),
(3, '复旦大学', 'https://picsum.photos/200/200?random=103', '上海市', '上海市', '杨浦区邯郸路220号', 3, 1),
(4, '浙江大学', 'https://picsum.photos/200/200?random=104', '浙江省', '杭州市', '西湖区余杭塘路866号', 4, 1),
(5, '武汉大学', 'https://picsum.photos/200/200?random=105', '湖北省', '武汉市', '武昌区八一路299号', 5, 1);

-- ----------------------------
-- 2. 管理员表测试数据
-- 密码 admin123 (BCrypt加密)
-- ----------------------------
INSERT INTO `admin` (`id`, `username`, `password`, `nickname`, `avatar`, `school_id`, `role`, `status`) VALUES
(1, 'admin', '$2b$10$cQonYD26llSmO3BSbEMHG.u2ZAcxf9gvN0GrLFSm584Q2Aag6jXSm', '超级管理员', 'https://picsum.photos/100/100?random=201', NULL, 2, 1),
(2, 'beida_admin', '$2b$10$cQonYD26llSmO3BSbEMHG.u2ZAcxf9gvN0GrLFSm584Q2Aag6jXSm', '北大管理员', 'https://picsum.photos/100/100?random=202', 1, 1, 1),
(3, 'qinghua_admin', '$2b$10$cQonYD26llSmO3BSbEMHG.u2ZAcxf9gvN0GrLFSm584Q2Aag6jXSm', '清华管理员', 'https://picsum.photos/100/100?random=203', 2, 1, 1);

-- ----------------------------
-- 3. 用户表测试数据
-- ----------------------------
INSERT INTO `user` (`id`, `openid`, `nickname`, `avatar`, `phone`, `school_id`, `status`) VALUES
(1, 'openid_zhangsan', '张三', 'https://picsum.photos/100/100?random=301', '13800138001', 1, 1),
(2, 'openid_lisi', '李四', 'https://picsum.photos/100/100?random=302', '13800138002', 1, 1),
(3, 'openid_wangwu', '王五', 'https://picsum.photos/100/100?random=303', '13800138003', 1, 1),
(4, 'openid_zhaoliu', '赵六', 'https://picsum.photos/100/100?random=304', '13800138004', 1, 1),
(5, 'openid_sunqi', '孙七', 'https://picsum.photos/100/100?random=305', '13800138005', 1, 1),
(6, 'openid_zhouba', '周八', 'https://picsum.photos/100/100?random=306', '13800138006', 1, 1),
(7, 'openid_wujiu', '吴九', 'https://picsum.photos/100/100?random=307', '13800138007', 1, 1),
(8, 'openid_zhengshi', '郑十', 'https://picsum.photos/100/100?random=308', '13800138008', 1, 1),
(9, 'openid_xiaomei', '小美', 'https://picsum.photos/100/100?random=309', '13800138009', 1, 1),
(10, 'openid_xiaoming', '小明', 'https://picsum.photos/100/100?random=310', '13800138010', 1, 1);

-- ----------------------------
-- 4. 轮播图表测试数据
-- ----------------------------
INSERT INTO `banner` (`id`, `school_id`, `title`, `image`, `link_type`, `link_url`, `sort`, `status`) VALUES
(1, 1, '欢迎来到智慧校园', 'https://picsum.photos/800/300?random=401', 0, NULL, 1, 1),
(2, 1, '快递代取新用户立减5元', 'https://picsum.photos/800/300?random=402', 1, '/pages/express/index', 2, 1),
(3, 1, '校园歌手大赛火热报名中', 'https://picsum.photos/800/300?random=403', 1, '/pages/activity/detail?id=1', 3, 1),
(4, 1, '二手市场开学季大促销', 'https://picsum.photos/800/300?random=404', 1, '/pages/goods/index', 4, 1),
(5, 1, '失物招领AI智能匹配上线', 'https://picsum.photos/800/300?random=405', 0, NULL, 5, 1);

-- ----------------------------
-- 5. 公告表测试数据
-- ----------------------------
INSERT INTO `notice` (`id`, `school_id`, `title`, `content`, `is_top`, `sort`, `status`) VALUES
(1, 1, '系统维护通知', '为提供更好的服务，系统将于本周六凌晨2:00-4:00进行维护升级，届时部分功能可能无法使用，敬请谅解。', 1, 1, 1),
(2, 1, '欢迎使用智慧校园小程序', '欢迎使用智慧校园小程序，这里有二手交易、快递代取、校园跑腿等多种便民服务，祝您使用愉快！', 1, 2, 1),
(3, 1, '校园快递代取服务上线', '校园快递代取服务正式上线啦！足不出户，快递送到宿舍楼下，新用户首单立减5元。', 0, 3, 1),
(4, 1, '关于规范二手交易的公告', '请各位同学在发布二手商品时遵守相关规定，禁止发布违禁物品，违者将被封号处理。', 0, 4, 1),
(5, 1, '寒假期间服务安排', '寒假期间（1月15日-2月20日）快递代取、跑腿等服务将调整运营时间，请合理安排使用。', 0, 5, 1),
(6, 1, '新功能：游戏搭子上线', '新增游戏搭子功能，找队友开黑再也不用愁啦，快来发布你的招募信息吧！', 0, 6, 1);

-- ----------------------------
-- 6. 商品分类表测试数据
-- ----------------------------
INSERT INTO `category` (`id`, `school_id`, `name`, `sort`) VALUES
(1, 1, '数码电子', 1),
(2, 1, '书籍教材', 2),
(3, 1, '生活用品', 3),
(4, 1, '服装鞋包', 4),
(5, 1, '运动器材', 5),
(6, 1, '其他', 99);

-- ----------------------------
-- 7. 二手商品表测试数据
-- ----------------------------
INSERT INTO `goods` (`id`, `user_id`, `school_id`, `title`, `description`, `price`, `images`, `category_id`, `status`, `audit_status`, `view_count`) VALUES
(1, 1, 1, '九成新iPad Air4 64G', '自用iPad Air4 64G 深空灰色，购买一年多，外观完好无划痕，配原装充电器和保护套，因换新机所以出掉。', 2800.00, 'https://picsum.photos/400/400?random=501,https://picsum.photos/400/400?random=502', 1, 0, 1, 156),
(2, 2, 1, '高等数学同济第七版上下册', '大一用过的高数教材，笔记齐全，适合预习和复习，书页干净无涂鸦。', 25.00, 'https://picsum.photos/400/400?random=503', 2, 0, 1, 89),
(3, 3, 1, '宿舍神器床上书桌', '大学四年必备床上书桌，可折叠，带抽屉和杯架，八成新，毕业带不走便宜出。', 35.00, 'https://picsum.photos/400/400?random=504,https://picsum.photos/400/400?random=505', 3, 0, 1, 234),
(4, 4, 1, 'Nike Air Force 1 42码', '白色空军一号，42码，穿了几次有点小，鞋盒都在，支持验货，非真假问题不退。', 450.00, 'https://picsum.photos/400/400?random=506', 4, 0, 1, 312),
(5, 5, 1, '迪卡侬山地自行车', '迪卡侬入门级山地车，26寸，骑了半年左右，刹车变速都正常，适合日常通勤。', 600.00, 'https://picsum.photos/400/400?random=507,https://picsum.photos/400/400?random=508', 5, 0, 1, 178),
(6, 6, 1, '小米手环7 NFC版', '小米手环7 NFC版，支持门禁卡和公交卡，屏幕完好，表带正常使用痕迹，充电器齐全。', 150.00, 'https://picsum.photos/400/400?random=509', 1, 0, 1, 95),
(7, 7, 1, '英语四级真题试卷', '星火英语四级真题，2023年版本，做了两套就考过了，剩下的基本全新，附答案解析。', 15.00, 'https://picsum.photos/400/400?random=510', 2, 1, 1, 67),
(8, 8, 1, '懒人沙发豆袋椅', '宿舍懒人沙发，豆袋椅，灰色，可拆洗，九成新，买了没怎么用，占地儿出了。', 80.00, 'https://picsum.photos/400/400?random=511', 3, 0, 1, 145),
(9, 9, 1, '罗技G304无线鼠标', '罗技G304无线游戏鼠标，黑色，使用半年，功能正常，手感好，续航给力。', 180.00, 'https://picsum.photos/400/400?random=512', 1, 0, 1, 76),
(10, 10, 1, '瑜伽垫加厚15mm', 'TPE瑜伽垫，加厚15mm，加长加宽，紫色，几乎全新，买了没用几次就闲置了。', 45.00, 'https://picsum.photos/400/400?random=513', 5, 2, 1, 52);

-- ----------------------------
-- 8. 帖子表测试数据
-- ----------------------------
INSERT INTO `post` (`id`, `user_id`, `school_id`, `title`, `content`, `images`, `view_count`, `audit_status`, `is_top`) VALUES
(1, 1, 1, '食堂三楼新开的麻辣香锅真不错', '今天去食堂三楼尝了尝新开的麻辣香锅，味道真的绝了！食材新鲜，味道正宗，价格也实惠，强烈推荐大家去试试，就在最里面那家。', 'https://picsum.photos/600/400?random=601', 520, 1, 0),
(2, 2, 1, '求推荐靠谱的考研机构', '准备25考研，想问问大家有没有推荐的考研机构？最好是线下的，数学和英语比较好的，价格不要太贵，感谢各位学长学姐！', NULL, 340, 1, 0),
(3, 3, 1, '图书馆占座现象太严重了', '真的受不了图书馆占座的人了，早上八点去就没位置了，好多座位上放着书但人一整天都不来，希望学校能管管这种现象。', NULL, 890, 1, 1),
(4, 4, 1, '校园里的流浪猫有人喂吗', '最近在宿舍楼下发现了一只橘猫，特别亲人，想问问有没有人组织喂猫的？我想加入，也想给它做个绝育。', 'https://picsum.photos/600/400?random=602', 256, 1, 0),
(5, 5, 1, '分享一下我的保研经验', '刚刚收到了北大的保研offer，想在这里分享一下我的保研经历，希望对学弟学妹们有帮助。首先成绩一定要好，其次多参加竞赛...', NULL, 1250, 1, 1),
(6, 6, 1, '有没有一起夜跑的小伙伴', '每天晚上9点左右在操场跑步，大概跑5公里，有没有想一起的小伙伴？可以互相监督打气，男女不限，速度不用太快。', NULL, 178, 1, 0),
(7, 7, 1, '吐槽一下学校的网', '宿舍的网也太慢了吧，打个游戏460延迟，看视频也卡，每个月交那么多网费就这？什么时候能改善一下啊。', NULL, 634, 1, 0),
(8, 8, 1, '捡到一张校园卡', '今天在一教门口捡到一张校园卡，是计算机学院的同学，名字就不说了，失主看到私信我认领。', 'https://picsum.photos/600/400?random=603', 89, 1, 0);

-- ----------------------------
-- 9. 评论表测试数据
-- ----------------------------
INSERT INTO `comment` (`id`, `post_id`, `user_id`, `content`, `parent_id`, `reply_user_id`) VALUES
(1, 1, 2, '确实好吃，我昨天也去了，就是人太多要排队', 0, NULL),
(2, 1, 3, '请问人均大概多少钱啊？', 0, NULL),
(3, 1, 1, '回复@王五：人均20左右吧，看你点多少菜', 2, 3),
(4, 2, 5, '推荐新东方，我去年报的，老师讲得挺好的', 0, NULL),
(5, 2, 6, '其实自学也可以，B站上有很多免费课程', 0, NULL),
(6, 3, 4, '太赞同了！占座真的很烦', 0, NULL),
(7, 3, 7, '建议学校搞个预约系统', 0, NULL),
(8, 5, 8, '学长太厉害了！能加个微信请教一下吗', 0, NULL),
(9, 5, 9, '请问夏令营需要准备什么材料啊', 0, NULL),
(10, 7, 10, '+1 我也卡得要死，打团就掉线', 0, NULL);

-- ----------------------------
-- 10. 点赞记录表测试数据
-- ----------------------------
INSERT INTO `like_record` (`id`, `target_id`, `target_type`, `user_id`) VALUES
(1, 1, 0, 2),
(2, 1, 0, 3),
(3, 1, 0, 4),
(4, 2, 0, 1),
(5, 3, 0, 1),
(6, 3, 0, 2),
(7, 3, 0, 5),
(8, 5, 0, 1),
(9, 5, 0, 2),
(10, 1, 1, 3);

-- ----------------------------
-- 11. 失物招领表测试数据
-- ----------------------------
INSERT INTO `lost_found` (`id`, `user_id`, `school_id`, `type`, `item_name`, `description`, `images`, `location`, `status`, `audit_status`) VALUES
(1, 1, 1, 0, '黑色双肩包', '今天下午在图书馆二楼自习室丢了一个黑色双肩包，里面有笔记本电脑和几本专业书，对我很重要，捡到必有重谢！', 'https://picsum.photos/400/300?random=701', '图书馆二楼', 0, 1),
(2, 2, 1, 1, '苹果AirPods Pro', '在操场跑步的时候捡到的，白色耳机盒，上面有个小恐龙贴纸，失主快来认领。', 'https://picsum.photos/400/300?random=702', '操场看台上', 0, 1),
(3, 3, 1, 0, '校园卡+钥匙', '昨天在食堂丢了校园卡和一串钥匙，钥匙上有个小熊挂件，补办卡太麻烦了，求好心人捡到联系我。', NULL, '一食堂一楼', 0, 1),
(4, 4, 1, 1, '雨伞', '一教门口捡到一把透明雨伞，伞柄是粉色的，下雨那天好多人丢伞，来认领的时候说一下伞的特征。', 'https://picsum.photos/400/300?random=703', '一教教学楼门口', 0, 1),
(5, 5, 1, 0, '粉色水杯', '我的粉色保温杯不见了，上面贴了很多贴纸，是我生日朋友送的，对我意义重大，在三教附近丢的。', 'https://picsum.photos/400/300?random=704', '三教教学楼', 0, 1),
(6, 6, 1, 1, '身份证', '在学校南门附近捡到一张身份证，是河南的同学，姓王，我已经交到保卫处了，失主可以去认领。', NULL, '学校南门', 1, 1),
(7, 7, 1, 0, 'switch游戏机', '上周在宿舍楼下的自习室丢了一台switch，蓝色的，里面有我好几个游戏存档，急死我了，找到必有重谢！', 'https://picsum.photos/400/300?random=705', '宿舍楼自习室', 0, 1),
(8, 8, 1, 1, '快递包裹', '菜鸟驿站拿错了一个快递，不是我的，是一个叫李同学的，包裹还没拆，我放在驿站前台了。', NULL, '菜鸟驿站', 1, 1);

-- ----------------------------
-- 12. 快递代取订单表测试数据
-- ----------------------------
INSERT INTO `express_order` (`id`, `order_no`, `school_id`, `user_id`, `picker_id`, `express_company`, `pickup_code`, `pickup_address`, `delivery_address`, `weight`, `receiver_name`, `receiver_phone`, `remark`, `service_fee`, `tip`, `total_amount`, `status`, `accept_time`, `complete_time`) VALUES
(1, 'EXP202401150001', 1, 1, 3, '顺丰速运', 'SF123456', '菜鸟驿站A区', '32号楼302室', 1, '张三', '13800138001', '放在门口就行', 5.00, 1.00, 6.00, 3, '2024-01-15 10:30:00', '2024-01-15 11:20:00'),
(2, 'EXP202401150002', 1, 2, NULL, '中通快递', 'ZT789012', '京东快递点', '28号楼501室', 0, '李四', '13800138002', '快递比较轻，麻烦快点', 3.00, 0.00, 3.00, 0, NULL, NULL),
(3, 'EXP202401150003', 1, 3, 5, '圆通速递', 'YT345678', '菜鸟驿站B区', '15号楼203室', 2, '王五', '13800138003', '大件，是个箱子，可能有点重', 8.00, 2.00, 10.00, 2, '2024-01-15 14:00:00', NULL),
(4, 'EXP202401150004', 1, 4, 6, '韵达快递', 'YD901234', '邮政快递点', '45号楼101室', 0, '赵六', '13800138004', NULL, 3.00, 0.00, 3.00, 1, '2024-01-15 15:30:00', NULL),
(5, 'EXP202401140005', 1, 5, 7, '京东物流', 'JD567890', '京东自提点', '36号楼402室', 1, '孙七', '13800138005', '请打电话叫我下来拿', 5.00, 1.00, 6.00, 3, '2024-01-14 09:00:00', '2024-01-14 10:15:00'),
(6, 'EXP202401140006', 1, 6, NULL, '申通快递', 'ST234567', '菜鸟驿站A区', '22号楼305室', 0, '周八', '13800138006', NULL, 3.00, 0.50, 3.50, 4, NULL, NULL),
(7, 'EXP202401130007', 1, 7, 8, '极兔速递', 'JT678901', '菜鸟驿站C区', '18号楼601室', 1, '吴九', '13800138007', '是衣服，轻拿轻放', 5.00, 0.00, 5.00, 3, '2024-01-13 16:00:00', '2024-01-13 17:00:00'),
(8, 'EXP202401130008', 1, 8, 2, '德邦快递', 'DB012345', '南门快递点', '8号楼102室', 2, '郑十', '13800138008', '大件重物，两个箱子', 10.00, 3.00, 13.00, 3, '2024-01-13 11:00:00', '2024-01-13 12:30:00');

-- ----------------------------
-- 13. 打印服务订单表测试数据
-- ----------------------------
INSERT INTO `print_order` (`id`, `order_no`, `school_id`, `user_id`, `printer_id`, `file_urls`, `print_type`, `paper_size`, `copies`, `is_double_side`, `binding`, `delivery_address`, `receiver_name`, `receiver_phone`, `remark`, `total_pages`, `total_amount`, `status`, `accept_time`, `complete_time`) VALUES
(1, 'PRT202401150001', 1, 1, 4, 'https://example.com/files/report1.pdf', 0, 'A4', 1, 1, 1, '32号楼302室', '张三', '13800138001', '毕业论文初稿，麻烦打印清晰点', 35, 7.00, 3, '2024-01-15 09:00:00', '2024-01-15 10:30:00'),
(2, 'PRT202401150002', 1, 2, NULL, 'https://example.com/files/notes.pdf', 0, 'A4', 2, 0, 0, '28号楼501室', '李四', '13800138002', '打印复习资料', 20, 8.00, 0, NULL, NULL),
(3, 'PRT202401150003', 1, 3, 5, 'https://example.com/files/ppt.pptx', 1, 'A4', 1, 0, 2, '15号楼203室', '王五', '13800138003', '答辩PPT，彩色打印，胶装', 25, 35.00, 2, '2024-01-15 13:00:00', NULL),
(4, 'PRT202401140004', 1, 4, 6, 'https://example.com/files/paper.pdf', 0, 'A4', 1, 1, 2, '45号楼101室', '赵六', '13800138004', '小论文，胶装成册', 15, 12.00, 3, '2024-01-14 10:00:00', '2024-01-14 15:00:00'),
(5, 'PRT202401140005', 1, 5, NULL, 'https://example.com/files/exam.pdf', 0, 'A3', 5, 0, 0, '36号楼402室', '孙七', '13800138005', '考研真题，单面打印', 12, 18.00, 0, NULL, NULL),
(6, 'PRT202401130006', 1, 6, 7, 'https://example.com/files/poster.jpg', 1, 'A3', 1, 0, 0, '22号楼305室', '周八', '13800138006', '活动海报，彩色打印', 1, 15.00, 3, '2024-01-13 14:00:00', '2024-01-13 16:00:00');

-- ----------------------------
-- 14. 校园跑腿订单表测试数据
-- ----------------------------
INSERT INTO `errand_order` (`id`, `order_no`, `school_id`, `user_id`, `runner_id`, `errand_type`, `title`, `description`, `pickup_address`, `delivery_address`, `receiver_name`, `receiver_phone`, `estimated_time`, `service_fee`, `goods_fee`, `tip`, `total_amount`, `status`, `accept_time`, `complete_time`) VALUES
(1, 'ERR202401150001', 1, 1, 3, 0, '帮买一杯奶茶', '帮我买一杯喜茶的多肉葡萄，少糖少冰，谢谢！', '学校东门喜茶店', '32号楼302室', '张三', '13800138001', '2024-01-15 15:00:00', 5.00, 28.00, 2.00, 35.00, 3, '2024-01-15 14:00:00', '2024-01-15 14:45:00'),
(2, 'ERR202401150002', 1, 2, NULL, 1, '送一份资料到教学楼', '有一份课程设计资料需要送到一教305教室，是给老师的，麻烦小心点别弄坏了。', '28号楼501室', '一教305教室', '李四', '13800138002', '2024-01-15 16:00:00', 8.00, 0.00, 0.00, 8.00, 0, NULL, NULL),
(3, 'ERR202401150003', 1, 3, 5, 2, '帮忙取个外卖', '外卖到了但是我在上课走不开，帮忙取一下送到三教201教室门口。', '学校南门外卖柜', '三教201教室', '王五', '13800138003', '2024-01-15 12:30:00', 5.00, 0.00, 1.00, 6.00, 2, '2024-01-15 12:10:00', NULL),
(4, 'ERR202401140004', 1, 4, 6, 0, '帮买感冒药', '感冒了不想动，帮忙去校医院买一盒999感冒灵和体温计，谢谢。', '校医院药房', '45号楼101室', '赵六', '13800138004', '2024-01-14 19:00:00', 6.00, 35.00, 4.00, 45.00, 3, '2024-01-14 18:30:00', '2024-01-14 19:10:00'),
(5, 'ERR202401140005', 1, 5, NULL, 3, '帮忙搬东西', '毕业要搬宿舍，东西有点多，找个力气大的同学帮忙搬一下，从5楼搬到楼下。', '12号楼502室', '学校南门', '孙七', '13800138005', '2024-01-14 10:00:00', 50.00, 0.00, 10.00, 60.00, 0, NULL, NULL),
(6, 'ERR202401130006', 1, 6, 7, 1, '送伞到图书馆', '下雨了没带伞，麻烦送一把伞到图书馆二楼，我在靠窗的位置。', '22号楼305室', '图书馆二楼', '周八', '13800138006', '2024-01-13 17:00:00', 8.00, 0.00, 2.00, 10.00, 3, '2024-01-13 16:40:00', '2024-01-13 17:05:00'),
(7, 'ERR202401130007', 1, 7, 8, 0, '帮买水果', '帮忙买两斤苹果和一串香蕉，要新鲜的，水果店在南门那边。', '南门水果店', '18号楼601室', '吴九', '13800138007', '2024-01-13 20:00:00', 5.00, 20.00, 0.00, 25.00, 1, '2024-01-13 19:30:00', NULL),
(8, 'ERR202401120008', 1, 8, 1, 2, '取快递送到宿舍', '有个大包裹在驿站，我一个人搬不动，帮忙取一下送到宿舍楼下。', '菜鸟驿站B区', '8号楼102室', '郑十', '13800138008', '2024-01-12 16:00:00', 10.00, 0.00, 5.00, 15.00, 3, '2024-01-12 15:30:00', '2024-01-12 16:20:00');

-- ----------------------------
-- 15. 代课服务订单表测试数据
-- ----------------------------
INSERT INTO `substitute_order` (`id`, `order_no`, `school_id`, `user_id`, `substitute_id`, `course_name`, `course_time`, `course_date`, `classroom`, `teacher`, `major`, `requirement`, `reward`, `contact_qq`, `contact_wechat`, `status`, `accept_time`, `complete_time`) VALUES
(1, 'SUB202401150001', 1, 1, 5, '高等数学', '周一第1-2节', '2024-01-15', '一教101', '张教授', '计算机科学与技术', '帮忙签到，坐在后排就行', 50.00, '123456789', 'zhangsan_wx', 2, '2024-01-14 20:00:00', '2024-01-15 10:00:00'),
(2, 'SUB202401160002', 1, 2, NULL, '大学英语', '周二第3-4节', '2024-01-16', '二教202', '李老师', '软件工程', '不要被老师点到回答问题', 40.00, '234567890', 'lisi_wx', 0, NULL, NULL),
(3, 'SUB202401170003', 1, 3, 6, '数据结构', '周三第5-6节', '2024-01-17', '三教303', '王教授', '计算机科学与技术', '帮忙记一下笔记', 60.00, '345678901', 'wangwu_wx', 1, '2024-01-16 10:00:00', NULL),
(4, 'SUB202401180004', 1, 4, NULL, '马克思主义原理', '周四第7-8节', '2024-01-18', '四教404', '赵老师', '机械工程', '只要人到了就行', 35.00, '456789012', 'zhaoliu_wx', 0, NULL, NULL),
(5, 'SUB202401120005', 1, 5, 7, '计算机网络', '周五第1-2节', '2024-01-12', '实验楼A501', '孙教授', '网络工程', '实验课，帮忙做一下实验', 80.00, '567890123', 'sunqi_wx', 2, '2024-01-11 18:00:00', '2024-01-12 10:30:00'),
(6, 'SUB202401190006', 1, 6, 8, '线性代数', '周一第3-4节', '2024-01-22', '一教105', '钱老师', '数学与应用数学', '坐中间位置，不要太显眼', 45.00, '678901234', 'zhouba_wx', 3, NULL, NULL);

-- ----------------------------
-- 16. 校园活动表测试数据
-- ----------------------------
INSERT INTO `activity` (`id`, `school_id`, `user_id`, `title`, `description`, `images`, `activity_type`, `activity_date`, `start_time`, `end_time`, `location`, `max_participants`, `current_participants`, `fee`, `contact_way`, `audit_status`, `status`, `view_count`, `is_top`) VALUES
(1, 1, 1, '校园歌手大赛', '一年一度的校园歌手大赛开始报名啦！展示你的歌喉，赢取丰厚奖品，欢迎所有喜欢唱歌的同学参加。', 'https://picsum.photos/600/400?random=801', 2, '2024-01-20', '18:00:00', '21:00:00', '大礼堂', 50, 32, 0.00, '微信：singer2024', 1, 1, 1250, 1),
(2, 1, 2, '3V3篮球挑战赛', '体育部组织的3V3篮球挑战赛，组队报名，冠军队伍有奖金和奖杯，欢迎篮球爱好者踊跃参加。', 'https://picsum.photos/600/400?random=802', 1, '2024-01-18', '09:00:00', '17:00:00', '篮球场', 64, 48, 0.00, 'QQ群：123456789', 1, 1, 890, 0),
(3, 1, 3, '考研经验分享会', '邀请成功保研和考研上岸的学长学姐分享经验，解答大家的疑问，有意向考研的同学不要错过。', 'https://picsum.photos/600/400?random=803', 0, '2024-01-17', '19:00:00', '21:00:00', '图书馆报告厅', 200, 156, 0.00, '微信：kaoyan_share', 1, 1, 2340, 1),
(4, 1, 4, '周末志愿服务活动', '本周末去敬老院做志愿服务，陪老人聊天、表演节目，弘扬志愿精神，欢迎有爱心的同学加入。', 'https://picsum.photos/600/400?random=804', 3, '2024-01-21', '08:00:00', '16:00:00', '阳光敬老院', 30, 18, 0.00, '微信：volunteer_pku', 1, 1, 456, 0),
(5, 1, 5, '新生编程马拉松', '面向大一新生的编程比赛，24小时内完成指定项目，锻炼编程能力和团队协作，丰厚奖品等你来拿。', 'https://picsum.photos/600/400?random=805', 0, '2024-01-25', '09:00:00', '18:00:00', '计算机学院楼', 100, 67, 0.00, '邮箱：hackathon@pku.edu.cn', 1, 1, 678, 0),
(6, 1, 6, '校园摄影大赛', '用镜头记录校园的美好瞬间，摄影大赛开始征稿啦，题材不限，获奖作品将在校园展出。', 'https://picsum.photos/600/400?random=806', 2, '2024-02-01', '00:00:00', '23:59:59', '线上活动', 0, 89, 0.00, '微信：photo_contest', 1, 1, 520, 0),
(7, 1, 7, '羽毛球友谊赛', '学院之间的羽毛球友谊赛，单打双打都有，以球会友，增进交流，欢迎羽毛球爱好者参加。', 'https://picsum.photos/600/400?random=807', 1, '2024-01-19', '14:00:00', '18:00:00', '羽毛球馆', 32, 24, 10.00, 'QQ：987654321', 1, 1, 345, 0),
(8, 1, 8, '桌游聚会', '周末桌游聚会，狼人杀、剧本杀、三国杀都有，认识新朋友，一起度过愉快的周末时光。', 'https://picsum.photos/600/400?random=808', 2, '2024-01-20', '14:00:00', '20:00:00', '学生活动中心', 20, 15, 20.00, '微信：boardgame_pku', 1, 1, 280, 0);

-- ----------------------------
-- 17. 活动报名表测试数据
-- ----------------------------
INSERT INTO `activity_signup` (`id`, `activity_id`, `user_id`, `name`, `phone`, `remark`, `status`) VALUES
(1, 1, 2, '李四', '13800138002', '想唱一首周杰伦的歌', 1),
(2, 1, 3, '王五', '13800138003', NULL, 1),
(3, 1, 4, '赵六', '13800138004', '报名参赛', 1),
(4, 2, 1, '张三', '13800138001', '我们队有三个人', 1),
(5, 2, 5, '孙七', '13800138005', '中锋位置', 1),
(6, 3, 6, '周八', '13800138006', '想咨询计算机考研', 1),
(7, 3, 7, '吴九', '13800138007', NULL, 1),
(8, 4, 8, '郑十', '13800138008', '之前有过志愿经历', 1),
(9, 4, 9, '小美', '13800138009', '可以表演节目', 2),
(10, 5, 10, '小明', '13800138010', '大一新生，想学习一下', 1);

-- ----------------------------
-- 18. 游戏搭子表测试数据
-- ----------------------------
INSERT INTO `game_buddy` (`id`, `school_id`, `user_id`, `game_name`, `title`, `description`, `game_level`, `play_mode`, `contact_way`, `play_time`, `gender`, `status`, `view_count`) VALUES
(1, 1, 1, '王者荣耀', '找个钻石以上的队友双排', '我是边路玩家，实力还行，找个打野或者射手双排上分，最好是钻石以上段位，能开语音最好。', '星耀', '排位赛', '微信：zhangsan_game', '晚上8点-11点', 2, 1, 234),
(2, 1, 2, '英雄联盟', '周末一起打灵活组排', '黄金段位，主打中单，周末有空的小伙伴一起玩，不喷人，开心游戏最重要。', '黄金', '灵活组排', 'QQ：123456789', '周六周日全天', 2, 1, 156),
(3, 1, 3, '原神', '找个一起刷本的搭子', '58级老玩家，什么本都能打，找个萌新或者回归玩家一起玩，带你刷圣遗物刷周本。', '58级', '联机副本', '微信：wangyu_genshin', '晚上7点-10点', 1, 1, 312),
(4, 1, 4, '和平精英', '四缺一，来个会玩的', '王牌段位，固定三缺一，来个猛男队友，最好会打狙的，晚上一起上分。', '王牌', '四排', 'QQ：987654321', '每天晚上', 0, 1, 278),
(5, 1, 5, 'CSGO', '找5黑队友打天梯', '双AK段位，打了好多年了，找几个队友一起打5黑天梯，娱乐为主，输赢不重要。', '双AK', '竞技模式', '微信：sunqi_csgo', '周末下午', 0, 1, 189),
(6, 1, 6, '动物森友会', '找个好友一起玩', '刚入手动森，岛上什么都没有，找个好友串门，互相送点水果和家具。', '新手', '休闲', '微信：zhouba_acnh', '随时都可以', 1, 1, 95),
(7, 1, 7, '永劫无间', '三排找队友', '陨星段位，主打妖刀姬，找两个队友一起三排上修罗，配合好就行。', '陨星', '三排', 'QQ：456789012', '晚上9点后', 2, 1, 145),
(8, 1, 8, '我的世界', '找建筑师一起建服务器', '想搞个生存服务器，找几个喜欢建筑的小伙伴一起玩，长期玩的来。', '资深玩家', '生存服务器', '微信：zhengshi_mc', '周末及假期', 2, 1, 210);

-- ----------------------------
-- 19. 系统消息表测试数据
-- ----------------------------
INSERT INTO `message` (`id`, `user_id`, `type`, `title`, `content`, `related_id`, `related_type`, `is_read`) VALUES
(1, 1, 0, '欢迎使用智慧校园', '欢迎使用智慧校园小程序，这里有丰富的校园服务等你体验！', NULL, NULL, 1),
(2, 1, 1, '快递订单已接单', '您的快递订单已被接单，预计30分钟内送达，请保持电话畅通。', 1, 'express_order', 1),
(3, 1, 2, '有人评论了你的帖子', '李四评论了你的帖子《食堂三楼新开的麻辣香锅真不错》：确实好吃，我昨天也去了...', 1, 'post', 0),
(4, 2, 0, '新功能上线通知', '游戏搭子功能上线啦，快去找队友开黑吧！', NULL, NULL, 1),
(5, 2, 1, '跑腿订单已完成', '您的跑腿订单已完成，满意的话记得给个好评哦~', 1, 'errand_order', 0),
(6, 3, 2, '你的帖子被点赞了', '你的帖子获得了10个点赞，继续加油！', 1, 'post', 1),
(7, 3, 1, '打印订单进行中', '您的打印订单正在打印中，预计1小时后送达。', 3, 'print_order', 0),
(8, 4, 0, '活动报名成功', '您已成功报名校园歌手大赛，请按时参加。', 1, 'activity', 1),
(9, 5, 2, '有人回复了你的评论', '张三回复了你的评论：人均20左右吧，看你点多少菜', 3, 'comment', 0),
(10, 6, 1, '代课订单取消', '您发布的代课订单已被取消，原因：找到朋友帮忙了。', 6, 'substitute_order', 1);

-- ----------------------------
-- 20. 聊天消息表测试数据
-- ----------------------------
INSERT INTO `chat_message` (`id`, `conversation_id`, `from_user_id`, `to_user_id`, `msg_type`, `content`, `is_read`) VALUES
(1, '1_2', 1, 2, 0, '你好，请问你发布的iPad还在吗？', 1),
(2, '1_2', 2, 1, 0, '在的，还没卖出去', 1),
(3, '1_2', 1, 2, 0, '能便宜点吗，2500行吗', 1),
(4, '1_2', 2, 1, 0, '最低2700，成色很好的，你可以先看看货', 0),
(5, '3_4', 3, 4, 0, '明天的快递麻烦10点前送到可以吗', 0),
(6, '3_4', 4, 3, 0, '好的，我尽量早点去取', 0),
(7, '5_6', 5, 6, 0, '考研资料打印好了吗', 1),
(8, '5_6', 6, 5, 0, '快了，正在装订，半小时后给你送过去', 1),
(9, '7_8', 7, 8, 0, '周末一起去打羽毛球吗', 1),
(10, '7_8', 8, 7, 0, '好啊，几点？在哪里集合', 0);

-- ----------------------------
-- 21. 用户反馈表测试数据
-- ----------------------------
INSERT INTO `feedback` (`id`, `school_id`, `user_id`, `type`, `title`, `content`, `contact`, `status`, `reply`, `reply_time`, `reply_admin_id`) VALUES
(1, 1, 1, 0, '建议增加匿名发帖功能', '希望论坛能增加匿名发帖的功能，有些话题不太好意思用真实身份发。', '13800138001', 2, '感谢您的建议，匿名功能已在开发计划中，预计下个版本上线，敬请期待！', '2024-01-10 14:30:00', 2),
(2, 1, 2, 1, '快递订单无法支付', '下单快递代取后，点击支付没反应，试了好几次都不行，希望尽快修复。', '13800138002', 1, NULL, NULL, NULL),
(3, 1, 3, 2, '有人发布虚假商品', '看到有人发布的二手商品明显是假的，价格低得离谱，希望管理员能审核严格一点。', '13800138003', 2, '感谢您的举报，我们已经下架了该商品并对发布者进行了警告处理，后续会加强审核。', '2024-01-12 10:00:00', 2),
(4, 1, 4, 0, '希望增加夜间模式', '晚上刷手机太亮了，希望能加个夜间模式或者深色主题，保护眼睛。', '13800138004', 0, NULL, NULL, NULL),
(5, 1, 5, 1, '头像上传失败', '换头像的时候一直上传失败，试了好多次都不行，图片也不大啊。', '13800138005', 2, '您好，该问题已修复，您可以重新尝试上传头像。如果仍有问题请联系客服。', '2024-01-13 16:00:00', 2),
(6, 1, 6, 3, '其他问题咨询', '我想咨询一下怎么成为跑腿员，需要什么条件吗？', '13800138006', 2, '您好，成为跑腿员需要完成实名认证并缴纳押金，具体可在跑腿页面点击"成为跑手"查看详情。', '2024-01-14 09:30:00', 2);

-- ----------------------------
-- 22. 操作日志表测试数据
-- ----------------------------
INSERT INTO `oper_log` (`id`, `user_id`, `operation`, `method`, `params`, `cost_time`) VALUES
(1, 1, '用户登录', 'com.campus.controller.UserController.login', '{"code":"xxx"}', 120),
(2, 1, '发布商品', 'com.campus.controller.GoodsController.publish', '{"title":"九成新iPad Air4...","price":2800}', 250),
(3, 2, '发布帖子', 'com.campus.controller.PostController.create', '{"title":"食堂三楼新开的麻辣香锅真不错"}', 180),
(4, 1, '下快递订单', 'com.campus.controller.ExpressOrderController.create', '{"expressCompany":"顺丰速运","price":6.00}', 320),
(5, 3, '接单', 'com.campus.controller.ExpressOrderController.accept', '{"orderId":1}', 95),
(6, 2, '活动报名', 'com.campus.controller.ActivitySignupController.signup', '{"activityId":1}', 150),
(7, NULL, '管理员登录', 'com.campus.controller.AdminController.login', '{"username":"admin"}', 80),
(8, 2, '审核商品', 'com.campus.controller.AdminGoodsController.audit', '{"goodsId":1,"status":1}', 60),
(9, 1, '发布失物招领', 'com.campus.controller.LostFoundController.create', '{"type":0,"itemName":"黑色双肩包"}', 200),
(10, 5, '提交反馈', 'com.campus.controller.FeedbackController.submit', '{"type":1,"title":"头像上传失败"}', 110);

SET FOREIGN_KEY_CHECKS = 1;

-- ============================
-- 数据统计
-- ============================
-- 表数量: 22 张
--   学校表: 5 条
--   管理员表: 3 条
--   用户表: 10 条
--   轮播图表: 5 条
--   公告表: 6 条
--   商品分类表: 6 条
--   二手商品表: 10 条
--   帖子表: 8 条
--   评论表: 10 条
--   点赞记录表: 10 条
--   失物招领表: 8 条
--   快递代取订单表: 8 条
--   打印服务订单表: 6 条
--   校园跑腿订单表: 8 条
--   代课服务订单表: 6 条
--   校园活动表: 8 条
--   活动报名表: 10 条
--   游戏搭子表: 8 条
--   系统消息表: 10 条
--   聊天消息表: 10 条
--   用户反馈表: 6 条
--   操作日志表: 10 条
-- 测试数据总计: 170 条
-- 管理员默认账号: admin / admin123
-- 所有业务数据 school_id 均关联到第一所学校 (id=1, 北京大学)
-- ============================
