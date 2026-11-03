-- ============================================================
-- SmartProcure – Phase 2 Migration
-- V6__create_approval_thresholds.sql
-- Configurable monetary approval levels
-- ============================================================

CREATE TABLE approval_thresholds (
    id            BIGSERIAL      PRIMARY KEY,
    level         INTEGER        NOT NULL,
    min_amount    NUMERIC(19,4)  NOT NULL,
    max_amount    NUMERIC(19,4),                -- NULL = unlimited
    required_role VARCHAR(50)    NOT NULL,
    description   VARCHAR(255),
    active        BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_approval_threshold_level UNIQUE (level),
    CONSTRAINT ck_approval_threshold_level CHECK (level > 0)
);

CREATE INDEX idx_approval_thresholds_level  ON approval_thresholds (level);
CREATE INDEX idx_approval_thresholds_active ON approval_thresholds (active);
