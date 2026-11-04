-- ============================================================
-- V13 — Approval Workflow Tables
-- ============================================================

-- Configurable amount thresholds per approval level
CREATE TABLE approval_thresholds (
    id            BIGSERIAL PRIMARY KEY,
    level         INTEGER        NOT NULL,          -- 1 = first approver, 2 = second, etc.
    min_amount    NUMERIC(15,2)  NOT NULL DEFAULT 0,
    max_amount    NUMERIC(15,2),                    -- NULL = no upper limit
    role_required VARCHAR(100)   NOT NULL,
    description   VARCHAR(255),
    active        BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TYPE approval_status AS ENUM (
    'PENDING',
    'APPROVED',
    'REJECTED',
    'DELEGATED',
    'SKIPPED'
);

-- One row per approval step per purchase request
CREATE TABLE approval_requests (
    id                   BIGSERIAL PRIMARY KEY,
    purchase_request_id  BIGINT       NOT NULL REFERENCES purchase_requests(id) ON DELETE CASCADE,
    threshold_id         BIGINT       REFERENCES approval_thresholds(id),
    level                INTEGER      NOT NULL,
    approver_id          BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    status               approval_status NOT NULL DEFAULT 'PENDING',
    comments             TEXT,
    actioned_at          TIMESTAMP WITH TIME ZONE,
    delegated_to_id      BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ar_pr_id     ON approval_requests(purchase_request_id);
CREATE INDEX idx_ar_approver  ON approval_requests(approver_id);
CREATE INDEX idx_ar_status    ON approval_requests(status);
CREATE INDEX idx_ar_level     ON approval_requests(level);

-- Seed default thresholds
INSERT INTO approval_thresholds (level, min_amount, max_amount, role_required, description) VALUES
(1,    0.00,     999.99,  'ROLE_MANAGER',         'Manager approval — up to $999'),
(2, 1000.00,   9999.99,   'ROLE_FINANCE',         'Finance approval — $1K to $9,999'),
(3, 10000.00,  NULL,      'ROLE_ADMIN',            'Executive approval — $10K and above');
