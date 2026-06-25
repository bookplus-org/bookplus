-- ============================================================
-- Book+ Inventory Service — Schema V1
-- ============================================================

-- ── Stock ─────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS stocks (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    book_id             UUID        NOT NULL UNIQUE,
    quantity_total      INTEGER     NOT NULL DEFAULT 0 CHECK (quantity_total >= 0),
    quantity_available  INTEGER     NOT NULL DEFAULT 0 CHECK (quantity_available >= 0),
    quantity_reserved   INTEGER     NOT NULL DEFAULT 0 CHECK (quantity_reserved >= 0),
    low_stock_threshold INTEGER     NOT NULL DEFAULT 5 CHECK (low_stock_threshold >= 0),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_stock_consistency
        CHECK (quantity_total = quantity_available + quantity_reserved)
);

CREATE INDEX idx_stocks_book_id ON stocks(book_id);

-- ── Stock Reservations ────────────────────────────────────────────────────

CREATE TYPE reservation_status AS ENUM ('PENDING', 'CONFIRMED', 'CANCELLED', 'EXPIRED');

CREATE TABLE IF NOT EXISTS stock_reservations (
    id          UUID                NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    book_id     UUID                NOT NULL,
    order_id    VARCHAR(100)        NOT NULL,
    user_id     VARCHAR(100)        NOT NULL,
    quantity    INTEGER             NOT NULL CHECK (quantity > 0),
    status      reservation_status  NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMPTZ         NOT NULL DEFAULT now(),
    expires_at  TIMESTAMPTZ         NOT NULL,
    resolved_at TIMESTAMPTZ
);

CREATE INDEX idx_reservations_book_id      ON stock_reservations(book_id);
CREATE INDEX idx_reservations_order_id     ON stock_reservations(order_id);
CREATE INDEX idx_reservations_status       ON stock_reservations(status);
CREATE INDEX idx_reservations_expires_at   ON stock_reservations(expires_at)
    WHERE status = 'PENDING';
CREATE INDEX idx_reservations_order_book   ON stock_reservations(order_id, book_id);

-- ── Stock Movements ───────────────────────────────────────────────────────

CREATE TYPE movement_type AS ENUM ('IN', 'OUT', 'RESERVED', 'UNRESERVED', 'ADJUSTMENT');

CREATE TABLE IF NOT EXISTS stock_movements (
    id           UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    book_id      UUID          NOT NULL,
    type         movement_type NOT NULL,
    quantity     INTEGER       NOT NULL CHECK (quantity > 0),
    stock_before INTEGER       NOT NULL,
    stock_after  INTEGER       NOT NULL,
    reference_id VARCHAR(100),
    notes        TEXT,
    occurred_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_movements_book_id     ON stock_movements(book_id);
CREATE INDEX idx_movements_occurred_at ON stock_movements(occurred_at DESC);
CREATE INDEX idx_movements_type        ON stock_movements(type);
CREATE INDEX idx_movements_reference   ON stock_movements(reference_id)
    WHERE reference_id IS NOT NULL;

-- ── Auto-update trigger ───────────────────────────────────────────────────

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_stocks_updated_at
    BEFORE UPDATE ON stocks
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
