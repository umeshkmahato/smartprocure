-- ============================================================
-- SmartProcure – Phase 2 Migration
-- V2__create_users.sql
-- Core user accounts table
-- ============================================================

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    department      VARCHAR(100),
    phone           VARCHAR(20),
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',

    -- JPA auditing columns (populated by SpringSecurityAuditorAware)
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),

    -- Optimistic locking
    version         BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email    UNIQUE (email),
    CONSTRAINT ck_users_status   CHECK  (status IN ('ACTIVE','INACTIVE','LOCKED','PENDING'))
);

-- Auth lookups
CREATE INDEX idx_users_email      ON users (email);
CREATE INDEX idx_users_username   ON users (username);

-- Admin / reporting filters
CREATE INDEX idx_users_status     ON users (status);
CREATE INDEX idx_users_department ON users (department);
