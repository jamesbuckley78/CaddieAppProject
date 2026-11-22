-- "Not coming in" requests: who approved/rejected them and when. approved stays NULL while
-- waiting for the admin. One request per caddie per day.
ALTER TABLE absences
    ADD COLUMN decided_by BIGINT   NULL AFTER approved,
    ADD COLUMN decided_at DATETIME NULL AFTER decided_by,
    ADD CONSTRAINT fk_absences_admin FOREIGN KEY (decided_by) REFERENCES users (id),
    ADD CONSTRAINT uq_absence_day UNIQUE (caddie_id, date);
