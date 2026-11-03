-- ============================================================
-- SmartProcure – Phase 3 Migration
-- V10__create_vendor_ratings.sql
-- Vendor performance ratings — ON DELETE SET NULL keeps history
-- ============================================================

CREATE TABLE vendor_ratings (
    id          BIGSERIAL  PRIMARY KEY,
    vendor_id   BIGINT     NOT NULL REFERENCES vendors (id) ON DELETE CASCADE,
    rated_by_id BIGINT     REFERENCES users (id) ON DELETE SET NULL,
    score       INTEGER    NOT NULL,
    comment     TEXT,
    created_at  TIMESTAMP  NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_vendor_ratings_score CHECK (score >= 1 AND score <= 5)
);

CREATE INDEX idx_vendor_ratings_vendor_id ON vendor_ratings (vendor_id);
CREATE INDEX idx_vendor_ratings_rated_by  ON vendor_ratings (rated_by_id);
CREATE INDEX idx_vendor_ratings_created   ON vendor_ratings (created_at DESC);
