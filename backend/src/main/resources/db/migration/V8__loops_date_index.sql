-- Bag counts, the live total and "who's out on course" filter loops by date only, which the
-- (caddie_id, date) index can't help with - without this every refresh reads every loop.
CREATE INDEX ix_loops_date ON loops (date);
