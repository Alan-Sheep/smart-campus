-- v5: 邮箱验证码登录
-- user 表新增 email 列 + 唯一索引（nullable：微信登录用户可以没有邮箱）
ALTER TABLE user ADD COLUMN email VARCHAR(128) NULL DEFAULT NULL COMMENT '登录邮箱' AFTER phone;
ALTER TABLE user ADD UNIQUE INDEX uk_user_email (email);