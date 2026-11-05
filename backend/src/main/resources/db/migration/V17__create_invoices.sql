-- ============================================================
-- V17 — Invoice Management
-- ============================================================

CREATE TABLE invoices (
    id               BIGSERIAL PRIMARY KEY,
    purchase_order_id BIGINT       NOT NULL REFERENCES purchase_orders(id),
    vendor_id         BIGINT       NOT NULL REFERENCES vendors(id),
    invoice_number    VARCHAR(60)  NOT NULL UNIQUE,
    status            VARCHAR(20)  NOT NULL DEFAULT 'SUBMITTED',
    total_amount      NUMERIC(15,2) NOT NULL,
    currency          VARCHAR(3)   NOT NULL DEFAULT 'USD',
    invoice_date      DATE         NOT NULL,
    due_date          DATE,
    document_url      VARCHAR(500),
    notes             TEXT,
    dispute_reason    TEXT,
    verified_at       TIMESTAMP,
    paid_at           TIMESTAMP,
    version           BIGINT       NOT NULL DEFAULT 0,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100)
);

CREATE INDEX idx_inv_po     ON invoices(purchase_order_id);
CREATE INDEX idx_inv_vendor ON invoices(vendor_id);
CREATE INDEX idx_inv_status ON invoices(status);

COMMENT ON TABLE  invoices                 IS 'Vendor-submitted invoices linked to purchase orders';
COMMENT ON COLUMN invoices.invoice_number  IS 'Unique vendor-generated invoice reference';
COMMENT ON COLUMN invoices.status          IS 'SUBMITTED | UNDER_REVIEW | VERIFIED | DISPUTED | PAID | CANCELLED';
COMMENT ON COLUMN invoices.document_url    IS 'URL or storage key to the uploaded invoice document';
COMMENT ON COLUMN invoices.dispute_reason  IS 'Populated by procurement officer when status = DISPUTED';
