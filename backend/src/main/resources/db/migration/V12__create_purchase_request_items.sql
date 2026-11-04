-- ============================================================
-- V12 — Purchase Request Line Items
-- ============================================================

CREATE TABLE purchase_request_items (
    id                   BIGSERIAL PRIMARY KEY,
    purchase_request_id  BIGINT        NOT NULL REFERENCES purchase_requests(id) ON DELETE CASCADE,
    item_name            VARCHAR(255)  NOT NULL,
    description          TEXT,
    quantity             NUMERIC(15,4) NOT NULL,
    unit                 VARCHAR(50),
    unit_price           NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    total_price          NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    vendor_id            BIGINT        REFERENCES vendors(id) ON DELETE SET NULL,
    notes                TEXT,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pri_pr_id    ON purchase_request_items(purchase_request_id);
CREATE INDEX idx_pri_vendor   ON purchase_request_items(vendor_id);
