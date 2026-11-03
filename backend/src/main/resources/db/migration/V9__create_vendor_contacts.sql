-- ============================================================
-- SmartProcure – Phase 3 Migration
-- V9__create_vendor_contacts.sql
-- ============================================================

CREATE TABLE vendor_contacts (
    id         BIGSERIAL    PRIMARY KEY,
    vendor_id  BIGINT       NOT NULL REFERENCES vendors (id) ON DELETE CASCADE,
    name       VARCHAR(100) NOT NULL,
    email      VARCHAR(255),
    phone      VARCHAR(20),
    position   VARCHAR(100),
    is_primary BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_vendor_contacts_vendor_id  ON vendor_contacts (vendor_id);
CREATE INDEX idx_vendor_contacts_primary    ON vendor_contacts (vendor_id, is_primary);
