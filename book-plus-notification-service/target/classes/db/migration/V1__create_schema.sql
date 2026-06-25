-- ============================================================
-- Notification Service — V1 schema
-- ============================================================

CREATE TYPE notification_type AS ENUM (
    'ORDER_CREATED', 'ORDER_CONFIRMED', 'ORDER_SHIPPED',
    'ORDER_DELIVERED', 'ORDER_CANCELLED',
    'PAYMENT_COMPLETED', 'PAYMENT_FAILED', 'PAYMENT_REFUNDED',
    'LOW_STOCK_ALERT', 'REVIEW_ADDED'
);

CREATE TYPE notification_channel AS ENUM ('EMAIL', 'SMS', 'PUSH');

CREATE TYPE notification_status AS ENUM ('PENDING', 'SENT', 'FAILED');

CREATE TABLE notifications (
    id              UUID                  PRIMARY KEY,
    user_id         VARCHAR(255)          NOT NULL,
    recipient_email VARCHAR(255)          NOT NULL,
    type            notification_type     NOT NULL,
    channel         notification_channel  NOT NULL DEFAULT 'EMAIL',
    subject         VARCHAR(512)          NOT NULL,
    body            TEXT,
    status          notification_status   NOT NULL DEFAULT 'PENDING',
    failure_reason  TEXT,
    reference_id    VARCHAR(255),
    created_at      TIMESTAMPTZ           NOT NULL DEFAULT NOW(),
    sent_at         TIMESTAMPTZ
);

CREATE INDEX idx_notifications_user_id    ON notifications (user_id);
CREATE INDEX idx_notifications_type       ON notifications (type);
CREATE INDEX idx_notifications_reference  ON notifications (reference_id);
CREATE INDEX idx_notifications_status     ON notifications (status);
CREATE INDEX idx_notifications_created_at ON notifications (created_at DESC);
