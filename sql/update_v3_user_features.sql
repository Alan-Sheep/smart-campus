-- ============================================================================
-- 用户端功能补全迁移脚本
-- ============================================================================
-- 内容：
--   1. post 表新增 category 分类列（推荐/吐槽/交友/分享）
--   2. 新建 collect 收藏表（支持商品/帖子/活动）
--   3. 学校数据改为重庆各高校
-- 说明：可重复执行（幂等）
-- ============================================================================

USE `smart_campus`;

-- ---------------------------------------------------------------------------
-- 1. post 新增 category 分类列
-- ---------------------------------------------------------------------------
-- 背景：校园频道顶部有「推荐/吐槽/交友/分享」分类标签，但 post 表没有分类字段，
--       导致点击分类只能重新加载全部帖子（分类筛选形同虚设）。
DROP PROCEDURE IF EXISTS `add_column_if_absent`;
DELIMITER $$
CREATE PROCEDURE `add_column_if_absent`(
    IN p_table  VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_ddl    VARCHAR(255)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table
          AND COLUMN_NAME = p_column
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN ', p_ddl);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL add_column_if_absent(
    'post', 'category',
    '`category` VARCHAR(16) NOT NULL DEFAULT ''推荐'' COMMENT ''分类(推荐/吐槽/交友/分享)'' AFTER `content`'
);

-- 为分类筛选建立索引（配合 school_id + audit_status 已有复合索引）
DROP PROCEDURE IF EXISTS `add_index_if_absent_2`;
DELIMITER $$
CREATE PROCEDURE `add_index_if_absent_2`(
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
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD INDEX `', p_index, '` (', p_cols, ')');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL add_index_if_absent_2('post', 'idx_school_cate_time', '`school_id`,`category`,`create_time`');

-- 给现有帖子按标题/内容分配合适的分类（让分类页有真实数据可看）
UPDATE `post` SET `category` = '吐槽' WHERE `category` = '推荐' AND (`title` LIKE '%占座%' OR `title` LIKE '%太严重%' OR `content` LIKE '%吐槽%' OR `content` LIKE '%太差%');
UPDATE `post` SET `category` = '交友' WHERE `category` = '推荐' AND (`title` LIKE '%流浪猫%' OR `title` LIKE '%搭子%' OR `content` LIKE '%一起%' OR `content` LIKE '%交友%');
UPDATE `post` SET `category` = '分享' WHERE `category` = '推荐' AND (`title` LIKE '%推荐%' OR `title` LIKE '%分享%' OR `title` LIKE '%经验%' OR `title` LIKE '%不错%' OR `title` LIKE '%好吃%' OR `content` LIKE '%推荐%' OR `content` LIKE '%分享%');

-- ---------------------------------------------------------------------------
-- 2. 新建 collect 收藏表
-- ---------------------------------------------------------------------------
-- 背景：用户端「我的收藏」页面是占位空壳，帖子详情也没有收藏入口。
--       like_record 只用于点赞，不复用，避免语义混淆。
CREATE TABLE IF NOT EXISTS `collect` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`     BIGINT      NOT NULL COMMENT '收藏用户ID',
  `target_id`   BIGINT      NOT NULL COMMENT '目标ID(商品/帖子/活动)',
  `target_type` TINYINT     NOT NULL COMMENT '目标类型(0:商品 1:帖子 2:活动)',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted`     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除(0:正常 1:删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_target` (`user_id`, `target_id`, `target_type`),
  KEY `idx_user_type` (`user_id`, `target_type`),
  KEY `idx_target` (`target_id`, `target_type`),
  CONSTRAINT `fk_collect_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏表';

-- ---------------------------------------------------------------------------
-- 3. 学校数据改为重庆各高校
-- ---------------------------------------------------------------------------
-- 先清理历史测试残留
DELETE FROM `school` WHERE `name` LIKE 'E2E%' OR `name` LIKE 'ZZ-Test%' OR `name` LIKE '%测试%';

-- 重命名现有 5 所学校为重庆高校（保留 id，不影响既有数据的 school_id 关联）
UPDATE `school` SET `name`='重庆大学',       `province`='重庆市', `city`='重庆市', `address`='沙坪坝区沙正街174号',       `sort`=1 WHERE `id`=1;
UPDATE `school` SET `name`='西南大学',       `province`='重庆市', `city`='重庆市', `address`='北碚区天生路2号',           `sort`=2 WHERE `id`=2;
UPDATE `school` SET `name`='重庆邮电大学',   `province`='重庆市', `city`='重庆市', `address`='南岸区崇文路2号',           `sort`=3 WHERE `id`=3;
UPDATE `school` SET `name`='重庆师范大学',   `province`='重庆市', `city`='重庆市', `address`='沙坪坝区大学城中路37号',    `sort`=4 WHERE `id`=4;
UPDATE `school` SET `name`='重庆交通大学',   `province`='重庆市', `city`='重庆市', `address`='南岸区学府大道66号',        `sort`=5 WHERE `id`=5;

-- 补充更多重庆高校（按名称判重，避免重复插入）
INSERT INTO `school` (`name`,`logo`,`province`,`city`,`address`,`sort`,`status`,`deleted`)
SELECT * FROM (
  SELECT '西南政法大学'   AS n,'https://picsum.photos/200/200?random=106' AS l,'重庆市' AS p,'重庆市' AS c,'渝北区宝圣大道301号'   AS a, 6 AS s,1 AS st,0 AS d
  UNION ALL SELECT '重庆医科大学','https://picsum.photos/200/200?random=107','重庆市','重庆市','渝中区医学院路1号',7,1,0
  UNION ALL SELECT '重庆理工大学','https://picsum.photos/200/200?random=108','重庆市','重庆市','巴南区红光大道69号',8,1,0
  UNION ALL SELECT '重庆工商大学','https://picsum.photos/200/200?random=109','重庆市','重庆市','南岸区学府大道19号',9,1,0
  UNION ALL SELECT '四川外国语大学','https://picsum.photos/200/200?random=110','重庆市','重庆市','沙坪坝区烈士墓壮志路33号',10,1,0
  UNION ALL SELECT '重庆科技大学','https://picsum.photos/200/200?random=111','重庆市','重庆市','沙坪坝区大学城东路20号',11,1,0
  UNION ALL SELECT '重庆文理学院','https://picsum.photos/200/200?random=112','重庆市','重庆市','永川区红河大道319号',12,1,0
  UNION ALL SELECT '长江师范学院','https://picsum.photos/200/200?random=113','重庆市','重庆市','涪陵区聚贤大道16号',13,1,0
  UNION ALL SELECT '重庆三峡学院','https://picsum.photos/200/200?random=114','重庆市','重庆市','万州区天星路666号',14,1,0
  UNION ALL SELECT '重庆第二师范学院','https://picsum.photos/200/200?random=115','重庆市','重庆市','南岸区学府大道9号',15,1,0
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM `school` s WHERE s.`name` = tmp.n);

DROP PROCEDURE IF EXISTS `add_column_if_absent`;
DROP PROCEDURE IF EXISTS `add_index_if_absent_2`;

SELECT '迁移完成' AS result;
SELECT id, name, province, city, sort FROM school ORDER BY sort;
