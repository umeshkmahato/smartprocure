-- ============================================================
-- SmartProcure – Phase 2 Migration
-- V3__create_roles.sql
-- Roles reference table + user↔role junction
-- ============================================================

CREATE TABLE roles (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL,
    description VARCHAR(255),

    CONSTRAINT uq_roles_name UNIQUE (name),
    CONSTRAINT ck_roles_name CHECK (name IN (
        'EMPLOYEE','MANAGER','PROCUREMENT_ADMIN','FINANCE','ADMIN'
    ))
);

-- Many-to-many junction: users ↔ roles
CREATE TABLE user_roles (
    user_id  BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id  BIGINT NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_user_id ON user_roles (user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles (role_id);
