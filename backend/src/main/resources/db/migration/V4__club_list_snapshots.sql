-- One row per daily club list update (4pm, docs/RULES.md). It records which bags were counted
-- (every loop up to max_loop_id), so the table keeps showing exactly the figures the order was
-- worked out from until the next update - bags entered after 4pm wait for tomorrow's update.
CREATE TABLE club_list_snapshots (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    taken_at     DATETIME NOT NULL,
    counts_up_to DATE     NOT NULL,
    max_loop_id  BIGINT   NOT NULL
);
