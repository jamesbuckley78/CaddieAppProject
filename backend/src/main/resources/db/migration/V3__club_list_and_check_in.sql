-- Club list position (docs/RULES.md, "Club list"): recalculated each night at midnight.
ALTER TABLE caddies ADD COLUMN list_position INT NULL;

-- Existing caddies: priority first, then in the order they were added.
UPDATE caddies c
    JOIN (SELECT id, ROW_NUMBER() OVER (ORDER BY is_priority DESC, id) AS rn FROM caddies) r ON r.id = c.id
SET c.list_position = r.rn;

ALTER TABLE caddies MODIFY list_position INT NOT NULL;
CREATE INDEX ix_caddies_list_position ON caddies (list_position);

-- queue_entries = one row per caddie per day they checked in.
-- Today's order is worked out live (docs/RULES.md, "Today's line"), so the stored position
-- becomes an optional manual override set by the admin (next feature).
ALTER TABLE queue_entries
    DROP INDEX ix_queue_day_position,
    CHANGE COLUMN position manual_position INT NULL,
    ADD COLUMN checked_in_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER status,
    ADD COLUMN back_at DATETIME NULL AFTER checked_in_at;
