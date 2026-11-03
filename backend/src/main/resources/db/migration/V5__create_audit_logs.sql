-- ============================================================
-- SmartProcure – Phase 2 Migration
-- V5__create_audit_logs.sql
-- Immutable audit trail for all critical business actions
-- ============================================================

CREATE TABLE audit_logs (
    id              BIGSERIAL    PRIMARY KEY,
    entity_type     VARCHAR(100) NOT NULL,
    entity_id       BIGINT,
    action          VARCHAR(50)  NOT NULL,
    performed_by    VARCHAR(100),
    performed_by_id BIGINT,
    old_value       TEXT,
    new_value       TEXT,
    ip_address      VARCHAR(45),
    correlation_id  VARCHAR(100),
    description     TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_entity        ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_logs_performed_by  ON audit_logs (performed_by_id);
CREATE INDEX idx_audit_logs_created_at    ON audit_logs (created_at);
CREATE INDEX idx_audit_logs_action        ON audit_logs (action);
CREATE INDEX idx_audit_logs_correlation   ON audit_logs (correlation_id);
