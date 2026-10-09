-- ============================
-- 智慧校园小程序 - 数据库升级脚本 V2
-- 说明：在 init.sql 基础上，补充缺失的表和字段
-- 执行前请先执行过 init.sql
-- ============================

USE smart_campus;

-- ----------------------------
-- 一、给现有表加字段
-- ----------------------------

-- 1. 用户表加学校ID和状态
ALTER TABLE `user` ADD COLUMN `school_id` BIGINT DEFAULT NULL COMMENT '学校ID' AFTER `phone`;
ALTER TABLE `user` ADD COLUMN `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态(0:禁用 1:正常)' AFTER `school_id`;
ALTER TABLE `user` ADD INDEX `idx_school_id` (`school_id`);

-- 2. 分类表加学校ID
ALTER TABLE `category` ADD COLUMN `school_id` BIGINT DEFAULT 0 COMMENT '学校ID(0表示全局)' AFTER `id`;
ALTER TABLE `category` ADD INDEX `idx_school_id` (`school_id`);

-- 3. 二手商品表加学校ID和审核状态
ALTER TABLE `goods` ADD COLUMN `school_id` BIGINT DEFAULT NULL COMMENT '学校ID' AFTER `user_id`;
ALTER TABLE `goods` ADD COLUMN `audit_status` TINYINT NOT NULL DEFAULT 1 COMMENT '审核状态(0:待审核 1:已通过 2:已驳回)' AFTER `status`;
ALTER TABLE `goods` ADD COLUMN `reject_reason` VARCHAR(256) DEFAULT NULL COMMENT '驳回原因' AFTER `audit_status`;
ALTER TABLE `goods` ADD INDEX `idx_school_id` (`school_id`);
ALTER TABLE `goods` ADD INDEX `idx_audit_status` (`audit_status`);

-- 4. 帖子表加学校ID、审核状态、置顶
ALTER TABLE `post` ADD COLUMN `school_id` BIGINT DEFAULT NULL COMMENT '学校ID' AFTER `user_id`;
ALTER TABLE `post` ADD COLUMN `audit_status` TINYINT NOT NULL DEFAULT 1 COMMENT '审核状态(0:待审核 1:已通过 2:已驳回)' AFTER `view_count`;
ALTER TABLE `post` ADD COLUMN `is_top` TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶(0:否 1:是)' AFTER `audit_status`;
ALTER TABLE `post` ADD COLUMN `reject_reason` VARCHAR(256) DEFAULT NULL COMMENT '驳回原因' AFTER `is_top`;
ALTER TABLE `post` ADD INDEX `idx_school_id` (`school_id`);
ALTER TABLE `post` ADD INDEX `idx_audit_status` (`audit_status`);

-- 5. 评论表加回复人ID
ALTER TABLE `comment` ADD COLUMN `reply_user_id` BIGINT DEFAULT NULL COMMENT '被回复用户ID' AFTER `parent_id`;

-- 6. 失物招领表加学校ID和审核状态
ALTER TABLE `lost_found` ADD COLUMN `school_id` BIGINT DEFAULT NULL COMMENT '学校ID' AFTER `user_id`;
ALTER TABLE `lost_found` ADD COLUMN `audit_status` TINYINT NOT NULL DEFAULT 1 COMMENT '审核状态(0:待审核 1:已通过 2:已驳回)' AFTER `status`;
ALTER TABLE `lost_found` ADD COLUMN `reject_reason` VARCHAR(256) DEFAULT NULL COMMENT '驳回原因' AFTER `audit_status`;
ALTER TABLE `lost_found` ADD INDEX `idx_school_id` (`school_id`);
ALTER TABLE `lost_found` ADD INDEX `idx_audit_status` (`audit_status`);

-- ----------------------------
-- 二、新增表
-- ----------------------------

-- 7. 学校表
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

-- 初始学校数据
INSERT INTO `school` (`name`, `province`, `city`, `sort`) VALUES
  ('示例大学', '北京市', '北京市', 1),
  ('清华大学', '北京市', '北京市', 2),
  ('北京大学', '北京市', '北京市', 3);

-- 8. 管理员表
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

-- 初始超级管理员 (密码: admin123，实际需加密存储，这里先给个默认)
INSERT INTO `admin` (`username`, `password`, `nickname`, `role`) VALUES
  ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '超级管理员', 2);

-- 9. 轮播图表
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

-- 初始轮播图数据
INSERT INTO `banner` (`school_id`, `title`, `image`, `sort`) VALUES
  (0, '欢迎使用智慧校园', 'https://picsum.photos/800/300?random=1', 1),
  (0, '快递代取优惠活动', 'https://picsum.photos/800/300?random=2', 2),
  (0, '校园活动火热报名中', 'https://picsum.photos/800/300?random=3', 3);

-- 10. 公告表
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

-- 初始公告数据
INSERT INTO `notice` (`school_id`, `title`, `content`, `is_top`) VALUES
  (0, '欢迎使用智慧校园小程序', '欢迎使用智慧校园小程序，祝您使用愉快！', 1),
  (0, '新功能上线', '失物招领AI匹配功能已上线，快来体验吧~', 0);

-- 11. 快递代取订单表
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

-- 12. 打印服务订单表
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

-- 13. 校园跑腿订单表
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

-- 14. 代课服务订单表
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

-- 15. 校园活动表
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

-- 16. 活动报名表
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

-- 17. 游戏搭子表
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

-- 18. 系统消息表
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

-- 19. 聊天消息表
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

-- 20. 用户反馈表
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
-- 三、给现有用户数据补学校ID
-- ----------------------------
UPDATE `user` SET `school_id` = 1 WHERE `school_id` IS NULL;
