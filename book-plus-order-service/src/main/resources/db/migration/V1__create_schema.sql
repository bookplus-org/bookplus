-- ============================================================
-- Order Service — V1 schema
-- ============================================================

CREATE TYPE order_status AS ENUM (
    'PENDING_PAYMENT',
    'PAYMENT_PROCESSING',
    'CONFIRMED',
    'SHIPPED',
    'DELIVERED',
    'CANCELLED'
);

-- ── orders ────────────────────────────────────────────────────────────────
CREATE TABLE orders (
    id                      UUID          PRIMARY KEY,
    user_id                 VARCHAR(255)  NOT NULL,
    cart_id                 VARCHAR(255)  NOT NULL,
    status                  order_status  NOT NULL DEFAULT 'PENDING_PAYMENT',
    total_amount            NUMERIC(12,2) NOT NULL CHECK (total_amount >= 0),
    total_currency          CHAR(3)       NOT NULL DEFAULT 'USD',

    -- Shipping address (snapshot — embedded, not normalized)
    shipping_recipient_name VARCHAR(255)  NOT NULL,
    shipping_street         VARCHAR(500)  NOT NULL,
    shipping_city           VARCHAR(255)  NOT NULL,
    shipping_state          VARCHAR(255),
    shipping_postal_code    VARCHAR(20)   NOT NULL,
    shipping_country        CHAR(3)       NOT NULL,

    payment_id              VARCHAR(255),
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_user_id    ON orders (user_id);
CREATE INDEX idx_orders_status     ON orders (status);
CREATE INDEX idx_orders_created_at ON orders (created_at DESC);

-- ── order_items ───────────────────────────────────────────────────────────
CREATE TABLE order_items (
    id          UUID          PRIMARY KEY,
    order_id    UUID          NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    book_id     VARCHAR(255)  NOT NULL,
    isbn        VARCHAR(20)   NOT NULL,
    title       VARCHAR(512)  NOT NULL,
    image_url   TEXT,
    unit_price  NUMERIC(12,2) NOT NULL CHECK (unit_price > 0),
    currency    CHAR(3)       NOT NULL DEFAULT 'USD',
    quantity    INT           NOT NULL CHECK (quantity > 0)
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);

-- ── Auto-update updated_at ─────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
