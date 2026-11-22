-- New accounts get a one-time setup code from the admin. Until the user picks their own
-- password, this flag stays TRUE and the app only lets them reach the set-password screen.
ALTER TABLE users
    ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT TRUE AFTER password_hash;
