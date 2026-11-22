-- =============================================================================
-- DEMO DATA - for local development only. Never run this on the real club database.
--
-- Adds 40 made-up caddies (phone numbers starting 0899, so real accounts are never
-- touched), bags spread over this week, and a mix of statuses for today: available,
-- getting food, will be back, out on course, finished for today, and not in.
-- Demo caddies can't log in (no password) - log in as the admin to see the board, or as
-- your own caddie account to see the club list.
--
-- Safe to run again: it deletes the previous demo data first, and all dates are worked
-- out from today, so it always fills the current week.
--
-- Run it (PowerShell, from the project folder):
--   Get-Content scripts\demo-data.sql | docker exec -i caddie-mysql mysql -ucaddie -pchange-me caddie
-- Remove it again: run scripts\demo-data-remove.sql the same way.
-- =============================================================================

SET @admin  := (SELECT id FROM users WHERE role = 'ADMIN' ORDER BY id LIMIT 1);
SET @monday := CURDATE() - INTERVAL WEEKDAY(CURDATE()) DAY;   -- WEEKDAY(): Monday = 0

-- ---- 1. Remove previous demo data -------------------------------------------
DELETE l FROM loops l JOIN caddies c ON c.id = l.caddie_id JOIN users u ON u.id = c.user_id WHERE u.phone LIKE '0899%';
DELETE q FROM queue_entries q JOIN caddies c ON c.id = q.caddie_id JOIN users u ON u.id = c.user_id WHERE u.phone LIKE '0899%';
DELETE a FROM attendance a JOIN caddies c ON c.id = a.caddie_id JOIN users u ON u.id = c.user_id WHERE u.phone LIKE '0899%';
DELETE c FROM caddies c JOIN users u ON u.id = c.user_id WHERE u.phone LIKE '0899%';
DELETE FROM users WHERE phone LIKE '0899%';

-- ---- 2. Demo caddies --------------------------------------------------------
-- n 1-10 are priority (a scaled-down "top 50"); 35-38 are juniors.
DROP TABLE IF EXISTS demo_names;
CREATE TABLE demo_names (n INT PRIMARY KEY, name VARCHAR(100), grade VARCHAR(10), priority BOOLEAN);
INSERT INTO demo_names VALUES
 (1,'Sean Murphy','DOUBLE',1),      (2,'Aoife Kelly','SINGLE',1),     (3,'Ciaran Byrne','DOUBLE',1),
 (4,'Niamh Ryan','SINGLE',1),       (5,'Padraig Walsh','DOUBLE',1),   (6,'Siobhan O''Brien','SINGLE',1),
 (7,'Darragh Doyle','DOUBLE',1),    (8,'Orla McCarthy','SINGLE',1),   (9,'Eoin Kennedy','DOUBLE',1),
 (10,'Grainne Lynch','SINGLE',1),   (11,'Conor Murray','SINGLE',0),   (12,'Ciara Quinn','DOUBLE',0),
 (13,'Fionn Moore','SINGLE',0),     (14,'Roisin Daly','SINGLE',0),    (15,'Oisin Brennan','DOUBLE',0),
 (16,'Saoirse Fitzgerald','SINGLE',0), (17,'Cathal Nolan','DOUBLE',0), (18,'Clodagh Power','SINGLE',0),
 (19,'Tadhg Gallagher','SINGLE',0), (20,'Maeve Carroll','DOUBLE',0),  (21,'Rory Brady','SINGLE',0),
 (22,'Sinead Hughes','SINGLE',0),   (23,'Declan Foley','DOUBLE',0),   (24,'Aisling Burke','SINGLE',0),
 (25,'Colm Reilly','SINGLE',0),     (26,'Deirdre Nugent','DOUBLE',0), (27,'Fergal Kavanagh','SINGLE',0),
 (28,'Eimear Whelan','SINGLE',0),   (29,'Kieran Hayes','DOUBLE',0),   (30,'Laoise Dunne','SINGLE',0),
 (31,'Brendan Healy','SINGLE',0),   (32,'Muireann Casey','DOUBLE',0), (33,'Shane Farrell','SINGLE',0),
 (34,'Una Maguire','SINGLE',0),     (35,'Jack Flynn','JUNIOR',0),     (36,'Ellie Sheridan','JUNIOR',0),
 (37,'Luke Tobin','JUNIOR',0),      (38,'Ava Corcoran','JUNIOR',0),   (39,'Niall Mulligan','DOUBLE',0),
 (40,'Tara Fitzpatrick-Moloney','SINGLE',0);

INSERT INTO users (name, phone, role, password_hash, must_change_password)
SELECT name, CONCAT('0899', LPAD(n, 6, '0')), 'CADDIE', NULL, 1 FROM demo_names;

-- Temporary positions after everyone else; the real order is worked out in step 5
INSERT INTO caddies (user_id, grade, is_priority, list_position)
SELECT u.id, d.grade, d.priority, 1000 + d.n
FROM demo_names d JOIN users u ON u.phone = CONCAT('0899', LPAD(d.n, 6, '0'));

-- A lookup of demo number -> caddie id, used below
DROP TABLE IF EXISTS demo_caddies;
CREATE TABLE demo_caddies AS
SELECT d.n, c.id AS caddie_id, d.grade
FROM demo_names d JOIN users u ON u.phone = CONCAT('0899', LPAD(d.n, 6, '0')) JOIN caddies c ON c.user_id = u.id;

-- ---- 3. Bags this week (Monday up to today) ---------------------------------
-- A made-up but repeatable pattern: roughly 2 bags in 5 days each, a few people with none,
-- double caddies carrying doubles on some days, and some days with two loops.
INSERT INTO loops (caddie_id, date, bag_type, tee_time, called_at, accepted_at, assigned_by)
WITH RECURSIVE days(k) AS (SELECT 0 UNION ALL SELECT k + 1 FROM days WHERE k < 6)
SELECT dc.caddie_id,
       @monday + INTERVAL days.k DAY,
       IF(dc.grade = 'DOUBLE' AND (dc.n + days.k) % 2 = 0, 'DOUBLE', 'SINGLE'),
       MAKETIME(7 + (dc.n * 3 + days.k) % 8, (dc.n * 10) % 60, 0),
       (@monday + INTERVAL days.k DAY) + INTERVAL (7 + (dc.n * 3 + days.k) % 8) HOUR,
       (@monday + INTERVAL days.k DAY) + INTERVAL (7 + (dc.n * 3 + days.k) % 8) HOUR + INTERVAL 3 MINUTE,
       @admin
FROM demo_caddies dc CROSS JOIN days
WHERE @monday + INTERVAL days.k DAY <= CURDATE()
  AND dc.n % 9 <> 0                              -- n = 9, 18, 27, 36: no bags all week
  AND (dc.n * 3 + days.k * 2) % 5 IN (0, 1);

-- A second loop on some days
INSERT INTO loops (caddie_id, date, bag_type, tee_time, called_at, accepted_at, assigned_by)
WITH RECURSIVE days(k) AS (SELECT 0 UNION ALL SELECT k + 1 FROM days WHERE k < 6)
SELECT dc.caddie_id, @monday + INTERVAL days.k DAY, 'SINGLE', '14:30:00',
       (@monday + INTERVAL days.k DAY) + INTERVAL 14 HOUR,
       (@monday + INTERVAL days.k DAY) + INTERVAL 14 HOUR + INTERVAL 5 MINUTE, @admin
FROM demo_caddies dc CROSS JOIN days
WHERE @monday + INTERVAL days.k DAY <= CURDATE()
  AND dc.n % 9 <> 0
  AND (dc.n + days.k) % 7 = 0;

-- ---- 4. Today's statuses ----------------------------------------------------
-- By demo number (n % 10): 0-1 not in today, 2 finished for today, 3 out on course,
-- 4 getting food, 5 will be back, 6-9 available.
INSERT INTO queue_entries (date, caddie_id, status, checked_in_at, back_at)
SELECT CURDATE(), dc.caddie_id,
       CASE dc.n % 10 WHEN 2 THEN 'OFF' WHEN 3 THEN 'OUT' WHEN 4 THEN 'FOOD' WHEN 5 THEN 'BACK_LATER'
                      ELSE 'AVAILABLE' END,
       CURDATE() + INTERVAL 7 HOUR + INTERVAL (dc.n * 4) MINUTE,
       IF(dc.n % 10 = 5, NOW() + INTERVAL (10 + dc.n) MINUTE, NULL)
FROM demo_caddies dc
WHERE dc.n % 10 NOT IN (0, 1);

-- Whoever is out on course is carrying a bag right now
INSERT INTO loops (caddie_id, date, bag_type, tee_time, called_at, accepted_at, assigned_by)
SELECT dc.caddie_id, CURDATE(), IF(dc.grade = 'DOUBLE', 'DOUBLE', 'SINGLE'),
       TIME(NOW() - INTERVAL 1 HOUR), NOW() - INTERVAL 70 MINUTE, NOW() - INTERVAL 65 MINUTE, @admin
FROM demo_caddies dc
WHERE dc.n % 10 = 3;

INSERT INTO attendance (caddie_id, date, checked_in_at, checked_out_at)
SELECT q.caddie_id, q.date, q.checked_in_at, IF(q.status = 'OFF', NOW() - INTERVAL 30 MINUTE, NULL)
FROM queue_entries q JOIN demo_caddies dc ON dc.caddie_id = q.caddie_id
WHERE q.date = CURDATE();

-- ---- 5. Club list order: same rule as the daily update (docs/RULES.md) ------
-- Groups: priority, other adults, juniors at the bottom. Within each: fewest bags this week,
-- ties keep the previous order.
UPDATE caddies c
JOIN (
    SELECT c2.id,
           ROW_NUMBER() OVER (ORDER BY c2.grade = 'JUNIOR', c2.is_priority DESC, COALESCE(b.bags, 0),
                                       c2.list_position, c2.id) AS rn
    FROM caddies c2
    LEFT JOIN (SELECT caddie_id, SUM(IF(bag_type = 'DOUBLE', 2, 1)) AS bags
               FROM loops WHERE date BETWEEN @monday AND CURDATE() GROUP BY caddie_id) b ON b.caddie_id = c2.id
) ranked ON ranked.id = c.id
SET c.list_position = ranked.rn;

-- Tell the club list screen these figures are current (only once the backend has created
-- the club_list_snapshots table - otherwise the backend's start-up catch-up does it).
SET @has_snapshots := (SELECT COUNT(*) FROM information_schema.tables
                       WHERE table_schema = DATABASE() AND table_name = 'club_list_snapshots');
SET @snapshot_sql := IF(@has_snapshots > 0,
    'INSERT INTO club_list_snapshots (taken_at, counts_up_to, max_loop_id) SELECT NOW(), CURDATE(), COALESCE(MAX(id), 0) FROM loops',
    'SELECT ''club_list_snapshots table not there yet - restart the backend to create it'' AS note');
PREPARE snapshot_stmt FROM @snapshot_sql;
EXECUTE snapshot_stmt;
DEALLOCATE PREPARE snapshot_stmt;

DROP TABLE demo_caddies;
DROP TABLE demo_names;

-- ---- Summary ----------------------------------------------------------------
SELECT COUNT(*) AS demo_caddies FROM users WHERE phone LIKE '0899%';
SELECT q.status, COUNT(*) AS caddies FROM queue_entries q JOIN caddies c ON c.id = q.caddie_id
JOIN users u ON u.id = c.user_id WHERE u.phone LIKE '0899%' AND q.date = CURDATE() GROUP BY q.status;
SELECT SUM(IF(bag_type = 'DOUBLE', 2, 1)) AS bags_this_week FROM loops WHERE date BETWEEN @monday AND CURDATE();
