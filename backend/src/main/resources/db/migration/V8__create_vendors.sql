-- ============================================================
-- SmartProcure – Phase 3 Migration
-- V8__create_vendors.sql
-- Vendor master table with approval lifecycle
-- ============================================================

CREATE TABLE vendors (
    id           BIGSERIAL     PRIMARY KEY,
    name         VARCHAR(255)  NOT NULL,
    email        VARCHAR(255)  NOT NULL,
    phone        VARCHAR(20),
    address      TEXT,
    tax_id       VARCHAR(50),
    status       VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    rating_score NUMERIC(3,2)  NOT NULL DEFAULT 0.00,
    active       BOOLEAN       NOT NULL DEFAULT TRUE,
    description  TEXT,
    website      VARCHAR(255),

    -- JPA auditing columns (matches AuditableEntity)
    created_at   TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP     NOT NULL DEFAULT NOW(),
    created_by   VARCHAR(100),
    updated_by   VARCHAR(100),
    version      BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT uq_vendors_email  UNIQUE (email),
    CONSTRAINT uq_vendors_tax_id UNIQUE (tax_id),
    CONSTRAINT ck_vendors_status CHECK (status IN (
        'PENDING','APPROVED','REJECTED','SUSPENDED','INACTIVE')),
    CONSTRAINT ck_vendors_rating CHECK (rating_score >= 0 AND rating_score <= 5)
);

CREATE INDEX idx_vendors_status  ON vendors (status);
CREATE INDEX idx_vendors_active  ON vendors (active);
CREATE INDEX idx_vendors_name    ON vendors (name);
CREATE INDEX idx_vendors_rating  ON vendors (rating_score DESC);
CREATE INDEX idx_vendors_created ON vendors (created_at DESC);
