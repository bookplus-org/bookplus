-- ============================================================
-- Payment Service — V1 schema
-- ============================================================

CREATE TYPE payment_status AS ENUM (
    'PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'REFUNDED'
);

CREATE TYPE payment_method AS ENUM (
    'CREDIT_CARD', 'DEBIT_CARD', 'PAYPAL', 'BANK_TRANSFER', 'CRYPTO'
);

CREATE TABLE payments (
    id                      UUID            PRIMARY KEY,
    order_id                VARCHAR(255)    NOT NULL UNIQUE,
    user_id                 VARCHAR(255)    NOT NULL,
    status                  payment_status  NOT NULL DEFAULT 'PENDING',
    amount                  NUMERIC(12,2)   NOT NULL CHECK (amount > 0),
    currency                CHAR(3)         NOT NULL DEFAULT 'USD',
    payment_method          payment_method  NOT NULL,
    gateway_transaction_ref VARCHAR(255),
    failure_reason          TEXT,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_order_id  ON payments (order_id);
CREATE INDEX idx_payments_user_id   ON payments (user_id);
CREATE INDEX idx_payments_status    ON payments (status);

CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN NEW.updated_at = NOW(); RETURN NEW; END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_payments_updated_at
    BEFORE UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
