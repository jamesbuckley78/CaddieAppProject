-- token_version: in every login token; bumping it (password change / reset) cancels older tokens.
-- setup_code_expires_at: one-time setup codes stop working after 48 hours.
-- push_provider: which push service push_token belongs to ("EXPO" now, "FCM" for a Flutter app).
ALTER TABLE users
    ADD COLUMN token_version         INT         NOT NULL DEFAULT 0 AFTER must_change_password,
    ADD COLUMN setup_code_expires_at DATETIME    NULL AFTER token_version,
    ADD COLUMN push_provider         VARCHAR(10) NULL AFTER push_token;
