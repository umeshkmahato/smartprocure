-- ============================================================
-- SmartProcure – Phase 2 Migration
-- V4__create_refresh_tokens.sql
-- JWT refresh token store with rotation support
-- ============================================================

CREATE TABLE refresh_tokens (
    id           BIGSERIAL    PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token        VARCHAR(512) NOT NULL,
    expires_at   TIMESTAMP    NOT NULL,
    revoked      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    ip_address   VARCHAR(45),
    user_agent   VARCHAR(500),

    CONSTRAINT uq_refresh_tokens_token UNIQUE (token)
);

-- Fast token lookup (validate/rotate path)
CREATE INDEX idx_refresh_tokens_token      ON refresh_tokens (token);
CREATE INDEX idx_refresh_tokens_user_id    ON refresh_tokens (user_id);
-- Cleanup job: remove expired tokens
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);
CREATE INDEX idx_refresh_tokens_revoked    ON refresh_tokens (revoked);
