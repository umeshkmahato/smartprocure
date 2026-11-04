-- ============================================================
-- V15 — Quotation Request & Quotation Tables
-- ============================================================

CREATE TYPE quotation_request_status AS ENUM (
    'DRAFT', 'PUBLISHED', 'CLOSED', 'CANCELLED'
);

CREATE TYPE quotation_status AS ENUM (
    'SUBMITTED', 'UNDER_REVIEW', 'ACCEPTED', 'REJECTED', 'EXPIRED'
);

CREATE TABLE quotation_requests (
    id                   BIGSERIAL PRIMARY KEY,
    rfq_number           VARCHAR(30)  NOT NULL UNIQUE,
    purchase_request_id  BIGINT       REFERENCES purchase_requests(id) ON DELETE SET NULL,
    title                VARCHAR(255) NOT NULL,
    description          TEXT,
    status               quotation_request_status NOT NULL DEFAULT 'DRAFT',
    deadline             TIMESTAMP WITH TIME ZONE NOT NULL,
    currency             VARCHAR(3)   NOT NULL DEFAULT 'USD',
    notes                TEXT,
    created_by_id        BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by           VARCHAR(100),
    updated_by           VARCHAR(100)
);

CREATE TABLE quotation_request_vendors (
    quotation_request_id BIGINT NOT NULL REFERENCES quotation_requests(id) ON DELETE CASCADE,
    vendor_id            BIGINT NOT NULL REFERENCES vendors(id) ON DELETE CASCADE,
    PRIMARY KEY (quotation_request_id, vendor_id)
);

CREATE TABLE quotations (
    id                   BIGSERIAL PRIMARY KEY,
    quotation_request_id BIGINT        NOT NULL REFERENCES quotation_requests(id) ON DELETE CASCADE,
    vendor_id            BIGINT        NOT NULL REFERENCES vendors(id) ON DELETE CASCADE,
    status               quotation_status NOT NULL DEFAULT 'SUBMITTED',
    total_amount         NUMERIC(15,2) NOT NULL,
    currency             VARCHAR(3)    NOT NULL DEFAULT 'USD',
    validity_date        DATE          NOT NULL,
    notes                TEXT,
    is_selected          BOOLEAN       NOT NULL DEFAULT FALSE,
    submitted_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    reviewed_at          TIMESTAMP WITH TIME ZONE,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE quotation_items (
    id             BIGSERIAL PRIMARY KEY,
    quotation_id   BIGINT        NOT NULL REFERENCES quotations(id) ON DELETE CASCADE,
    item_name      VARCHAR(255)  NOT NULL,
    description    TEXT,
    quantity       NUMERIC(15,4) NOT NULL,
    unit           VARCHAR(50),
    unit_price     NUMERIC(15,2) NOT NULL,
    total_price    NUMERIC(15,2) NOT NULL,
    notes          TEXT
);

CREATE INDEX idx_qr_status       ON quotation_requests(status);
CREATE INDEX idx_qr_pr_id        ON quotation_requests(purchase_request_id);
CREATE INDEX idx_q_rfq_id        ON quotations(quotation_request_id);
CREATE INDEX idx_q_vendor_id     ON quotations(vendor_id);
CREATE INDEX idx_q_status        ON quotations(status);
CREATE INDEX idx_qi_quotation_id ON quotation_items(quotation_id);
