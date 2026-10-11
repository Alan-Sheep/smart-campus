USE smart_campus;
UPDATE post SET images=CONCAT('https://picsum.photos/seed/post', id, '/600/400') WHERE deleted=0 AND audit_status=1 AND (images IS NULL OR images='');
UPDATE lost_found SET images=CONCAT('https://loremflickr.com/400/300/object?lock=', id) WHERE deleted=0 AND (images IS NULL OR images='');
UPDATE activity SET images=CONCAT('https://loremflickr.com/800/500/campus,event?lock=', id) WHERE deleted=0 AND (images IS NULL OR images='');