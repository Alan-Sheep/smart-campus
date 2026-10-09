-- ============================================================================
-- 性能优化：为高频列表查询补充复合索引
-- ============================================================================
-- 背景：各模块列表接口的共同查询模式是
--   WHERE school_id = ? AND status = ? ORDER BY create_time DESC
-- 现有索引都是「单列索引」，MySQL 只能用到其中一列，另一列与排序需回表/文件排序。
-- 复合索引 (school_id, status, create_time) 可同时覆盖过滤与排序。
--
-- 说明：
-- 1. 全部使用 IF NOT EXISTS 思路（MySQL 8 不支持 CREATE INDEX IF NOT EXISTS，
--    故用存储过程判断，可重复执行、幂等）。
-- 2. 只增不改不删，对现有数据与业务逻辑零影响。
-- 3. 数据量小的时候提升不明显，数据增长后避免全表扫描 + filesort。
-- ============================================================================

USE `smart_campus`;

DROP PROCEDURE IF EXISTS `add_index_if_absent`;
DELIMITER $$
CREATE PROCEDURE `add_index_if_absent`(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_cols  VARCHAR(255)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table
          AND INDEX_NAME = p_index
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD INDEX `', p_index, '` (', p_cols, ')');
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- 二手商品列表：school_id + status，按 create_time 倒序
CALL add_index_if_absent('goods', 'idx_school_status_time', '`school_id`,`status`,`create_time`');
-- 校园频道帖子列表：school_id + audit_status（已通过），按 create_time 倒序
CALL add_index_if_absent('post', 'idx_school_audit_time', '`school_id`,`audit_status`,`create_time`');
-- 失物招领列表：school_id + audit_status + type
CALL add_index_if_absent('lost_found', 'idx_school_audit_time', '`school_id`,`audit_status`,`create_time`');
-- 校园活动列表：school_id + status，按 create_time 倒序
CALL add_index_if_absent('activity', 'idx_school_status_time', '`school_id`,`status`,`create_time`');
-- 游戏搭子列表：school_id + status，按 create_time 倒序
CALL add_index_if_absent('game_buddy', 'idx_school_status_time', '`school_id`,`status`,`create_time`');
-- 跑腿大厅：school_id + status，按 create_time 倒序
CALL add_index_if_absent('errand_order', 'idx_school_status_time', '`school_id`,`status`,`create_time`');
-- 快递大厅：school_id + status，按 create_time 倒序
CALL add_index_if_absent('express_order', 'idx_school_status_time', '`school_id`,`status`,`create_time`');
-- 代课列表：school_id + status，按 create_time 倒序
CALL add_index_if_absent('substitute_order', 'idx_school_status_time', '`school_id`,`status`,`create_time`');
-- 打印订单：school_id + status
CALL add_index_if_absent('print_order', 'idx_school_status_time', '`school_id`,`status`,`create_time`');
-- 公告列表：school_id + status，按 create_time 倒序
CALL add_index_if_absent('notice', 'idx_school_status_time', '`school_id`,`status`,`create_time`');
-- 评论按帖子查询并倒序
CALL add_index_if_absent('comment', 'idx_post_time', '`post_id`,`create_time`');
-- 点赞统计：批量按 target_id + target_type 聚合
CALL add_index_if_absent('like_record', 'idx_target_type', '`target_id`,`target_type`');

DROP PROCEDURE IF EXISTS `add_index_if_absent`;

SELECT '复合索引优化完成' AS result;
