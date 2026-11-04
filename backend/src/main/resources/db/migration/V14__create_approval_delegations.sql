-- ============================================================
-- V14 — Approval Delegations
-- ============================================================

CREATE TABLE approval_delegations (
    id            BIGSERIAL PRIMARY KEY,
    delegator_id  BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    delegate_id   BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    start_date    DATE         NOT NULL,
    end_date      DATE         NOT NULL,
    reason        TEXT,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_delegation_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_no_self_delegation CHECK (delegator_id <> delegate_id)
);

CREATE INDEX idx_del_delegator ON approval_delegations(delegator_id);
CREATE INDEX idx_del_delegate  ON approval_delegations(delegate_id);
CREATE INDEX idx_del_dates     ON approval_delegations(start_date, end_date);
