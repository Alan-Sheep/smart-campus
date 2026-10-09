-- ============================================================
-- 智慧校园数据库 - 外键约束补全脚本
-- 说明：所有表使用逻辑删除（deleted字段），外键约束用于保证数据完整性
-- 策略：school_id / user_id 等主外键用 ON DELETE RESTRICT
--       可选关联字段（如 runner_id、substitute_id）用 ON DELETE SET NULL
-- ============================================================

-- 先把没有索引的外键字段补上索引（外键必须有索引）
ALTER TABLE comment ADD INDEX idx_user_id (user_id);
ALTER TABLE comment ADD INDEX idx_parent_id (parent_id);
ALTER TABLE comment ADD INDEX idx_reply_user_id (reply_user_id);
ALTER TABLE errand_order ADD INDEX idx_runner_id (runner_id);
ALTER TABLE express_order ADD INDEX idx_picker_id (picker_id);
ALTER TABLE feedback ADD INDEX idx_reply_admin_id (reply_admin_id);
ALTER TABLE like_record ADD INDEX idx_user_id (user_id);
ALTER TABLE oper_log ADD INDEX idx_user_id (user_id);
ALTER TABLE print_order ADD INDEX idx_printer_id (printer_id);
ALTER TABLE substitute_order ADD INDEX idx_substitute_id (substitute_id);
ALTER TABLE chat_message ADD INDEX idx_from_user_id (from_user_id);
ALTER TABLE chat_message ADD INDEX idx_to_user_id (to_user_id);

-- ============================================================
-- 一、以 school 表为核心的外键（所有业务表都关联学校）
-- ============================================================
ALTER TABLE user ADD CONSTRAINT fk_user_school FOREIGN KEY (school_id) REFERENCES school (id) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE admin ADD CONSTRAINT fk_admin_school FOREIGN KEY (school_id) REFERENCES school (id) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE category ADD CONSTRAINT fk_category_school FOREIGN KEY (school_id) REFERENCES school (id) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE banner ADD CONSTRAINT fk_banner_school FOREIGN KEY (school_id) REFERENCES school (id) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE notice ADD CONSTRAINT fk_notice_school FOREIGN KEY (school_id) REFERENCES school (id) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE post ADD CONSTRAINT fk_post_school FOREIGN KEY (school_id) REFERENCES school (id) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE goods ADD CONSTRAINT fk_goods_school FOREIGN KEY (school_id) REFERENCES school (id) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE activity ADD CONSTRAINT fk_activity_school FOREIGN KEY (school_id) REFERENCES school (id) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE errand_order ADD CONSTRAINT fk_errand_school FOREIGN KEY (school_id) REFERENCES school (id) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE express_order ADD CONSTRAINT fk_express_school FOREIGN KEY (school_id) REFERENCES school (id) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE substitute_order ADD CONSTRAINT fk_substitute_school FOREIGN KEY (school_id) REFERENCES school (id) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE game_buddy ADD CONSTRAINT fk_game_school FOREIGN KEY (school_id) REFERENCES school (id) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE lost_found ADD CONSTRAINT fk_lost_found_school FOREIGN KEY (school_id) REFERENCES school (id) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE print_order ADD CONSTRAINT fk_print_school FOREIGN KEY (school_id) REFERENCES school (id) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE feedback ADD CONSTRAINT fk_feedback_school FOREIGN KEY (school_id) REFERENCES school (id) ON DELETE RESTRICT ON UPDATE CASCADE;

-- ============================================================
-- 二、以 user 表为核心的外键（发布者、接单者、评论者等）
-- ============================================================

-- 帖子 - 发布者
ALTER TABLE post ADD CONSTRAINT fk_post_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE RESTRICT ON UPDATE CASCADE;

-- 评论 - 帖子、发布者、父评论、被回复者
ALTER TABLE comment ADD CONSTRAINT fk_comment_post FOREIGN KEY (post_id) REFERENCES post (id) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE comment ADD CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE comment ADD CONSTRAINT fk_comment_parent FOREIGN KEY (parent_id) REFERENCES comment (id) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE comment ADD CONSTRAINT fk_comment_reply_user FOREIGN KEY (reply_user_id) REFERENCES user (id) ON DELETE SET NULL ON UPDATE CASCADE;

-- 点赞记录 - 用户
ALTER TABLE like_record ADD CONSTRAINT fk_like_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE ON UPDATE CASCADE;

-- 商品 - 发布者、分类
ALTER TABLE goods ADD CONSTRAINT fk_goods_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE goods ADD CONSTRAINT fk_goods_category FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE RESTRICT ON UPDATE CASCADE;

-- 活动 - 发布者
ALTER TABLE activity ADD CONSTRAINT fk_activity_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE RESTRICT ON UPDATE CASCADE;

-- 活动报名 - 活动、用户
ALTER TABLE activity_signup ADD CONSTRAINT fk_signup_activity FOREIGN KEY (activity_id) REFERENCES activity (id) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE activity_signup ADD CONSTRAINT fk_signup_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE RESTRICT ON UPDATE CASCADE;

-- 跑腿订单 - 发布者、接单者
ALTER TABLE errand_order ADD CONSTRAINT fk_errand_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE errand_order ADD CONSTRAINT fk_errand_runner FOREIGN KEY (runner_id) REFERENCES user (id) ON DELETE SET NULL ON UPDATE CASCADE;

-- 快递代取 - 发布者、接单者
ALTER TABLE express_order ADD CONSTRAINT fk_express_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE express_order ADD CONSTRAINT fk_express_picker FOREIGN KEY (picker_id) REFERENCES user (id) ON DELETE SET NULL ON UPDATE CASCADE;

-- 代课服务 - 发布者、代课者
ALTER TABLE substitute_order ADD CONSTRAINT fk_substitute_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE substitute_order ADD CONSTRAINT fk_substitute_sub FOREIGN KEY (substitute_id) REFERENCES user (id) ON DELETE SET NULL ON UPDATE CASCADE;

-- 游戏搭子 - 发布者
ALTER TABLE game_buddy ADD CONSTRAINT fk_game_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE RESTRICT ON UPDATE CASCADE;

-- 失物招领 - 发布者
ALTER TABLE lost_found ADD CONSTRAINT fk_lost_found_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE RESTRICT ON UPDATE CASCADE;

-- 打印订单 - 发布者
ALTER TABLE print_order ADD CONSTRAINT fk_print_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE RESTRICT ON UPDATE CASCADE;

-- 反馈 - 用户、回复管理员
ALTER TABLE feedback ADD CONSTRAINT fk_feedback_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE feedback ADD CONSTRAINT fk_feedback_admin FOREIGN KEY (reply_admin_id) REFERENCES admin (id) ON DELETE SET NULL ON UPDATE CASCADE;

-- 消息 - 用户
ALTER TABLE message ADD CONSTRAINT fk_message_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE ON UPDATE CASCADE;

-- 聊天消息 - 发送者、接收者
ALTER TABLE chat_message ADD CONSTRAINT fk_chat_from_user FOREIGN KEY (from_user_id) REFERENCES user (id) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE chat_message ADD CONSTRAINT fk_chat_to_user FOREIGN KEY (to_user_id) REFERENCES user (id) ON DELETE RESTRICT ON UPDATE CASCADE;

-- 操作日志 - 用户
ALTER TABLE oper_log ADD CONSTRAINT fk_oper_log_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE SET NULL ON UPDATE CASCADE;
