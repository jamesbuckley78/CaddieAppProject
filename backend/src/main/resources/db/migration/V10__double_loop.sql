-- Double loop (docs/RULES.md): the caddie went round twice (36 holes) on the same bag. Only
-- marked when the admin chooses to; a marked loop counts twice (a double bag on one = 4 bags).
ALTER TABLE loops
    ADD COLUMN double_loop BOOLEAN NOT NULL DEFAULT FALSE AFTER bag_type;
