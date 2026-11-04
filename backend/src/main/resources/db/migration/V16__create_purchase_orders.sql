-- ============================================================
-- V16 — Purchase Orders
-- ============================================================

CREATE TYPE purchase_order_status AS ENUM (
    'DRAFT', 'ISSUED', 'ACKNOWLEDGED', 'PARTIALLY_DELIVERED',
    'DELIVERED', 'INVOICED', 'CLOSED', 'CANCELLED'
);

CREATE TABLE purchase_orders (
    id                   BIGSERIAL PRIMARY KEY,
    po_number            VARCHAR(30)  NOT NULL UNIQUE,
    purchase_request_id  BIGINT       REFERENCES purchase_requests(id) ON DELETE SET NULL,
    quotation_id         BIGINT       REFERENCES quotations(id) ON DELETE SET NULL,
    vendor_id            BIGINT       NOT NULL REFERENCES vendors(id) ON DELETE RESTRICT,
    status               purchase_order_status NOT NULL DEFAULT 'DRAFT',
    issued_by_id         BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    total_amount         NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    currency             VARCHAR(3)   NOT NULL DEFAULT 'USD',
    delivery_date        DATE,
    delivery_address     TEXT,
    payment_terms        VARCHAR(255),
    notes                TEXT,
    version              BIGINT       NOT NULL DEFAULT 0,
    issued_at            TIMESTAMP WITH TIME ZONE,
    acknowledged_at      TIMESTAMP WITH TIME ZONE,
    delivered_at         TIMESTAMP WITH TIME ZONE,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by           VARCHAR(100),
    updated_by           VARCHAR(100)
);

CREATE TABLE purchase_order_items (
    id               BIGSERIAL PRIMARY KEY,
    purchase_order_id BIGINT        NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    item_name        VARCHAR(255)  NOT NULL,
    description      TEXT,
    quantity         NUMERIC(15,4) NOT NULL,
    unit             VARCHAR(50),
    unit_price       NUMERIC(15,2) NOT NULL,
    total_price      NUMERIC(15,2) NOT NULL,
    delivered_qty    NUMERIC(15,4) NOT NULL DEFAULT 0,
    notes            TEXT
);

CREATE INDEX idx_po_status    ON purchase_orders(status);
CREATE INDEX idx_po_vendor    ON purchase_orders(vendor_id);
CREATE INDEX idx_po_pr_id     ON purchase_orders(purchase_request_id);
CREATE INDEX idx_po_number    ON purchase_orders(po_number);
CREATE INDEX idx_poi_po_id    ON purchase_order_items(purchase_order_id);
