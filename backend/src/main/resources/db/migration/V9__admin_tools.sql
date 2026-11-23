-- Caddies (and admins) who leave are marked as left instead of deleted, so their bags and the
-- audit log still point at them. A left account can't log in and is off the club list
-- (list_position 0) until an admin brings them back.
ALTER TABLE users
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE AFTER role;

-- Issue reports: who dealt with each one, and when.
ALTER TABLE issue_reports
    ADD COLUMN resolved_by BIGINT   NULL AFTER status,
    ADD COLUMN resolved_at DATETIME NULL AFTER resolved_by,
    ADD CONSTRAINT fk_issues_admin FOREIGN KEY (resolved_by) REFERENCES users (id),
    ADD INDEX ix_issues_status (status);
