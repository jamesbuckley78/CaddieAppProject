-- Calling a caddie (docs/RULES.md, "Calling"). While a call is waiting for the caddie, the
-- bag type and times live on today's queue entry; the loop row is only written when the
-- caddie is actually sent out, so the loops table only ever holds bags that were carried.
ALTER TABLE queue_entries
    ADD COLUMN called_bag_type  VARCHAR(10) NULL AFTER back_at,
    ADD COLUMN called_at        DATETIME    NULL AFTER called_bag_type,
    ADD COLUMN call_accepted_at DATETIME    NULL AFTER called_at,
    ADD COLUMN missed_calls     INT         NOT NULL DEFAULT 0 AFTER call_accepted_at;

-- When the caddie came back in, for their history.
ALTER TABLE loops
    ADD COLUMN returned_at DATETIME NULL AFTER accepted_at;

CREATE INDEX ix_loops_caddie_date ON loops (caddie_id, date);
