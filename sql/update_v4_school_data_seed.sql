-- ============================================================================
-- 按学校隔离的数据补齐脚本（v4）
-- ============================================================================
-- 背景：把学校改名为「重庆各高校」时，只改了 school.name，
--       但按 school_id 隔离的业务数据仍全部挂在 school_id=1，
--       导致切换到其他学校后 公告/轮播图/商品/活动/失物招领/游戏搭子 全是空的。
--
-- 本脚本补齐：notice(公告)、category(商品分类)、goods(商品)、
--             activity(活动)、lost_found(失物招领)
-- （banner / game_buddy 另行补齐）
--
-- 幂等：可重复执行，按「school_id + 判重键」判重，不会插重。
-- ============================================================================

USE `smart_campus`;

-- 恢复被误删的数据（历史测试遗留）
UPDATE notice   SET deleted=0 WHERE deleted=1;
UPDATE banner   SET deleted=0 WHERE deleted=1;

-- 1. 公告 notice
INSERT INTO notice (school_id, title, content, is_top, sort, status, deleted)
SELECT s.id, n.title, n.content, n.is_top,
       ROW_NUMBER() OVER (PARTITION BY s.id ORDER BY n.sort), 1, 0
FROM school s
CROSS JOIN (SELECT * FROM notice WHERE school_id=1 AND deleted=0) n
WHERE s.deleted=0 AND s.status=1 AND s.id <> 1
  AND NOT EXISTS (SELECT 1 FROM notice x WHERE x.school_id=s.id AND x.deleted=0);

-- 2. 商品分类 category（无 status 列）
INSERT INTO category (school_id, name, sort, deleted)
SELECT s.id, c.name,
       ROW_NUMBER() OVER (PARTITION BY s.id ORDER BY c.sort), 0
FROM school s
CROSS JOIN (SELECT * FROM category WHERE school_id=1 AND deleted=0) c
WHERE s.deleted=0 AND s.status=1 AND s.id <> 1
  AND NOT EXISTS (SELECT 1 FROM category x WHERE x.school_id=s.id AND x.name=c.name AND x.deleted=0);

-- 3. 商品 goods（category_id 改绑到各校自己的分类）
INSERT INTO goods (school_id, user_id, title, description, price, images, category_id, status, audit_status, view_count, deleted)
SELECT s.id, g.user_id, CONCAT(g.title,'（',s.name,'）'), g.description, g.price, g.images,
       nc.id, g.status, g.audit_status, 0, 0
FROM school s
CROSS JOIN (SELECT * FROM goods WHERE school_id=1 AND deleted=0) g
JOIN category oc ON oc.id = g.category_id AND oc.deleted=0
JOIN category nc ON nc.school_id = s.id AND nc.name = oc.name AND nc.deleted=0
WHERE s.deleted=0 AND s.status=1 AND s.id <> 1
  AND NOT EXISTS (SELECT 1 FROM goods x WHERE x.school_id=s.id AND x.title=CONCAT(g.title,'（',s.name,'）'));

-- 4. 校园活动 activity
INSERT INTO activity (school_id, user_id, title, description, images, activity_type,
                      activity_date, start_time, end_time, location, max_participants,
                      current_participants, fee, contact_way, audit_status, status, view_count, is_top, deleted)
SELECT s.id, a.user_id, CONCAT(a.title,'（',s.name,'）'), a.description, a.images, a.activity_type,
       a.activity_date, a.start_time, a.end_time, a.location, a.max_participants,
       0, a.fee, a.contact_way, 1, 1, 0, 0, 0
FROM school s
CROSS JOIN (SELECT * FROM activity WHERE school_id=1 AND deleted=0) a
WHERE s.deleted=0 AND s.status=1 AND s.id <> 1
  AND NOT EXISTS (SELECT 1 FROM activity x WHERE x.school_id=s.id AND x.title=CONCAT(a.title,'（',s.name,'）'));

-- 5. 失物招领 lost_found
INSERT INTO lost_found (school_id, user_id, type, item_name, description, images, location, status, audit_status, deleted)
SELECT s.id, l.user_id, l.type, CONCAT(l.item_name,'（',s.name,'）'), l.description, l.images, l.location, l.status, 1, 0
FROM school s
CROSS JOIN (SELECT * FROM lost_found WHERE school_id=1 AND deleted=0) l
WHERE s.deleted=0 AND s.status=1 AND s.id <> 1
  AND NOT EXISTS (SELECT 1 FROM lost_found x WHERE x.school_id=s.id AND x.item_name=CONCAT(l.item_name,'（',s.name,'）'));