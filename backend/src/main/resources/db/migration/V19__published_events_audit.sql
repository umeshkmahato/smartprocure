-- ============================================================
-- V19 — Published Events Audit Log
-- ============================================================
-- Records every domain event successfully published to Kafka.
-- Provides ops visibility, replay capability, and an audit trail
-- without blocking the publishing thread (insert happens async).
-- ============================================================

CREATE TABLE IF NOT EXISTS published_events (
    id           BIGSERIAL    PRIMARY KEY,
    event_id     VARCHAR(36)  NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    topic        VARCHAR(200) NOT NULL,
    payload      JSONB        NOT NULL,
    published_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_published_events_event_id UNIQUE (event_id)
);

CREATE INDEX idx_published_events_event_type   ON published_events (event_type);
CREATE INDEX idx_published_events_published_at ON published_events (published_at DESC);

COMMENT ON TABLE  published_events                IS 'Audit log of all domain events published to Kafka';
COMMENT ON COLUMN published_events.event_id       IS 'UUID from the event record — globally unique';
COMMENT ON COLUMN published_events.event_type     IS 'e.g. VendorApprovedEvent, PurchaseOrderIssuedEvent';
COMMENT ON COLUMN published_events.topic          IS 'Kafka topic the event was sent to';
COMMENT ON COLUMN published_events.payload        IS 'Full event JSON for replay / debugging';
COMMENT ON COLUMN published_events.published_at   IS 'Wall-clock time the publish succeeded';
