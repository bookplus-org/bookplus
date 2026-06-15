-- ============================================================
-- Report Service — V1 schema
-- ============================================================

CREATE TABLE daily_sales (
    id              BIGSERIAL     PRIMARY KEY,
    sale_date       DATE          NOT NULL UNIQUE,
    orders_count    INT           NOT NULL DEFAULT 0,
    items_sold      INT           NOT NULL DEFAULT 0,
    revenue         NUMERIC(14,2) NOT NULL DEFAULT 0,
    currency        CHAR(3)       NOT NULL DEFAULT 'USD',
    cancellations   INT           NOT NULL DEFAULT 0,
    refunds         INT           NOT NULL DEFAULT 0,
    refunded_amount NUMERIC(14,2) NOT NULL DEFAULT 0
);

CREATE INDEX idx_daily_sales_date ON daily_sales (sale_date DESC);

CREATE TABLE order_events (
    id          BIGSERIAL     PRIMARY KEY,
    order_id    VARCHAR(255)  NOT NULL,
    user_id     VARCHAR(255)  NOT NULL,
    event_type  VARCHAR(40)   NOT NULL,
    total       NUMERIC(12,2) NOT NULL DEFAULT 0,
    currency    CHAR(3)       NOT NULL DEFAULT 'USD',
    items_json  TEXT,
    occurred_on TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_order_events_order_id    ON order_events (order_id);
CREATE INDEX idx_order_events_user_id     ON order_events (user_id);
CREATE INDEX idx_order_events_occurred_on ON order_events (occurred_on DESC);
CREATE INDEX idx_order_events_type        ON order_events (event_type);
