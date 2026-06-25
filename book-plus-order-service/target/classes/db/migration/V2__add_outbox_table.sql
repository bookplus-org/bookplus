-- ============================================================
-- Transactional Outbox — V2
-- Events are written here atomically with the aggregate save.
-- The OutboxRelay reads PENDING rows and publishes to Kafka.
-- ============================================================

CREATE TABLE outbox_events (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type  VARCHAR(100)  NOT NULL,           -- e.g. "Order"
    aggregate_id    VARCHAR(255)  NOT NULL,            -- orderId
    event_type      VARCHAR(100)  NOT NULL,            -- e.g. "OrderCreatedEvent"
    topic           VARCHAR(255)  NOT NULL,            -- Kafka topic
    partition_key   VARCHAR(255)  NOT NULL,            -- Kafka partition key
    payload         TEXT          NOT NULL,            -- JSON serialized event
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',  -- PENDING | PUBLISHED | FAILED
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    published_at    TIMESTAMPTZ,
    retry_count     INT           NOT NULL DEFAULT 0,
    last_error      TEXT
);

CREATE INDEX idx_outbox_status      ON outbox_events (status, created_at);
CREATE INDEX idx_outbox_aggregate   ON outbox_events (aggregate_id);
