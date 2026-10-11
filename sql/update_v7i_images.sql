USE smart_campus;
-- 失物：全部重写为基于自身 id 的唯一图片 URL（lock=id 保证不重复）
UPDATE lost_found SET images=CONCAT('https://picsum.photos/seed/lost', id, '/400/300') WHERE deleted=0;
-- 活动：同样重写
UPDATE activity SET images=CONCAT('https://picsum.photos/seed/act', id, '/800/500') WHERE deleted=0;
-- 商品：重写为基于自身 id
UPDATE goods SET images=CONCAT('https://picsum.photos/seed/goods', id, '/400/400,https://picsum.photos/seed/goods', id, 'b/400/400') WHERE deleted=0;
-- 帖子：基于自身 id
UPDATE post SET images=CONCAT('https://picsum.photos/seed/post', id, '/600/400') WHERE deleted=0 AND (images IS NULL OR images='' OR images LIKE '%random=%');
-- 活动报名图片检查跳过（无图字段）