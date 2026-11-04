-- ============================================================
-- V11 — Purchase Requests
-- ============================================================

CREATE TYPE purchase_request_status AS ENUM (
    'DRAFT',
    'SUBMITTED',
    'UNDER_REVIEW',
    'APPROVED',
    'REJECTED',
    'CANCELLED',
    'ORDERED'
);

CREATE TYPE purchase_request_priority AS ENUM (
    'LOW',
    'MEDIUM',
    'HIGH',
    'URGENT'
);

CREATE TABLE purchase_requests (
    id               BIGSERIAL PRIMARY KEY,
    pr_number        VARCHAR(30)  NOT NULL UNIQUE,
    title            VARCHAR(255) NOT NULL,
    description      TEXT,
    status           purchase_request_status    NOT NULL DEFAULT 'DRAFT',
    priority         purchase_request_priority  NOT NULL DEFAULT 'MEDIUM',
    requester_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    department       VARCHAR(100),
    required_by_date DATE,
    total_amount     NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    currency         VARCHAR(3)   NOT NULL DEFAULT 'USD',
    rejection_reason TEXT,
    notes            TEXT,
    version          BIGINT       NOT NULL DEFAULT 0,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100)
);

CREATE INDEX idx_pr_status        ON purchase_requests(status);
CREATE INDEX idx_pr_requester     ON purchase_requests(requester_id);
CREATE INDEX idx_pr_priority      ON purchase_requests(priority);
CREATE INDEX idx_pr_created_at    ON purchase_requests(created_at DESC);
CREATE INDEX idx_pr_number        ON purchase_requests(pr_number);
