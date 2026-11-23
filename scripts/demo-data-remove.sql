-- Removes everything scripts/demo-data.sql added (caddies with phone numbers starting 0899).
-- Real accounts are not touched. The club list order tidies itself up at the next 4pm update.
--
--   Get-Content scripts\demo-data-remove.sql | docker exec -i caddie-mysql mysql -ucaddie -pchange-me caddie

DELETE l FROM loops l JOIN caddies c ON c.id = l.caddie_id JOIN users u ON u.id = c.user_id WHERE u.phone LIKE '0899%';
DELETE q FROM queue_entries q JOIN caddies c ON c.id = q.caddie_id JOIN users u ON u.id = c.user_id WHERE u.phone LIKE '0899%';
DELETE a FROM attendance a JOIN caddies c ON c.id = a.caddie_id JOIN users u ON u.id = c.user_id WHERE u.phone LIKE '0899%';
DELETE i FROM issue_reports i JOIN caddies c ON c.id = i.caddie_id JOIN users u ON u.id = c.user_id WHERE u.phone LIKE '0899%';
DELETE a FROM absences a JOIN caddies c ON c.id = a.caddie_id JOIN users u ON u.id = c.user_id WHERE u.phone LIKE '0899%';
DELETE c FROM caddies c JOIN users u ON u.id = c.user_id WHERE u.phone LIKE '0899%';
DELETE FROM users WHERE phone LIKE '0899%';

SELECT COUNT(*) AS caddies_left FROM caddies;
