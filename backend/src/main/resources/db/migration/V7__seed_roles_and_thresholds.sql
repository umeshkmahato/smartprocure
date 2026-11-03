-- ============================================================
-- SmartProcure – Phase 2 Migration
-- V7__seed_roles_and_thresholds.sql
-- Reference data — idempotent (ON CONFLICT DO NOTHING)
-- Admin user is created by DataInitializer at application startup
-- ============================================================

-- ── Roles ─────────────────────────────────────────────────────────────────
INSERT INTO roles (name, description) VALUES
    ('EMPLOYEE',          'Can submit purchase requests and track their status'),
    ('MANAGER',           'Can approve purchase requests within configured thresholds'),
    ('PROCUREMENT_ADMIN', 'Manages vendors, quotation requests, and purchase orders'),
    ('FINANCE',           'Manages invoice processing, payment verification, and financial reporting'),
    ('ADMIN',             'Full system access: user management, configuration, audit logs')
ON CONFLICT (name) DO NOTHING;

-- ── Approval thresholds ────────────────────────────────────────────────────
-- Amounts stored as NUMERIC(19,4). NULL max_amount = unlimited ceiling.
INSERT INTO approval_thresholds (level, min_amount, max_amount, required_role, description) VALUES
    (1, 0.0000,     9999.9999,  'MANAGER', 'Level 1 — Direct Manager approval (up to $10,000)'),
    (2, 10000.0000, 49999.9999, 'MANAGER', 'Level 2 — Department Head approval ($10,000 to $50,000)'),
    (3, 50000.0000, NULL,       'FINANCE', 'Level 3 — Finance Director approval (over $50,000)')
ON CONFLICT (level) DO NOTHING;
