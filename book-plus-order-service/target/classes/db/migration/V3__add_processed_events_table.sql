-- ============================================================
-- Idempotent Consumers — V3
-- Records every Kafka message key that has been successfully processed.
-- Before processing, consumers check this table to skip duplicates.
-- ============================================================

CREATE TABLE processed_events (
    id           BIGSERIAL    PRIMARY KEY,
    event_id     VARCHAR(255) NOT NULL,   -- Kafka message key (orderId, paymentId, etc.)
    topic        VARCHAR(255) NOT NULL,
    processed_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_processed_events UNIQUE (event_id, topic)
);

CREATE INDEX idx_processed_events_at ON processed_events (processed_at DESC);
