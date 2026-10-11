USE smart_campus;

-- 评论：每篇帖子 2-3 条
INSERT INTO comment (post_id, user_id, content, create_time)
SELECT p.id, u.id, c.ct, NOW() - INTERVAL FLOOR(RAND()*72) HOUR
FROM post p
JOIN (SELECT id FROM user WHERE deleted=0 ORDER BY RAND() LIMIT 10) u ON RAND()<0.5
JOIN (SELECT 'ct' k, '评论1' ct UNION ALL SELECT 'k','评论2' UNION ALL SELECT 'k','评论3') c ON RAND()<0.5
WHERE p.deleted=0 AND p.audit_status=1
AND NOT EXISTS (SELECT 1 FROM comment x WHERE x.post_id=p.id AND x.user_id=u.id AND x.content=c.ct AND x.deleted=0);

-- 点赞
INSERT INTO like_record (user_id, target_id, target_type, create_time)
SELECT u.id, p.id, 0, NOW() - INTERVAL FLOOR(RAND()*168) HOUR
FROM post p
JOIN (SELECT id FROM user WHERE deleted=0 ORDER BY RAND() LIMIT 20) u ON RAND()<0.35
WHERE p.deleted=0 AND p.audit_status=1
AND NOT EXISTS (SELECT 1 FROM like_record x WHERE x.user_id=u.id AND x.target_id=p.id AND x.target_type=0 AND x.deleted=0);

-- 收藏
INSERT INTO collect (user_id, target_id, target_type, create_time)
SELECT u.id, p.id, 1, NOW() - INTERVAL FLOOR(RAND()*168) HOUR
FROM post p
JOIN (SELECT id FROM user WHERE deleted=0 ORDER BY RAND() LIMIT 12) u ON RAND()<0.2
WHERE p.deleted=0 AND p.audit_status=1
AND NOT EXISTS (SELECT 1 FROM collect x WHERE x.user_id=u.id AND x.target_id=p.id AND x.target_type=1);