-- ============================================================
-- Catalog Service — V9: proyección de compras (biblioteca del usuario)
-- ============================================================
-- Poblada por un consumidor Kafka de order.payment.confirmed.
-- Da acceso al PDF completo del libro a quien lo compró.

CREATE TABLE IF NOT EXISTS user_purchases (
    user_id      VARCHAR(255) NOT NULL,
    book_id      UUID         NOT NULL REFERENCES books (id) ON DELETE CASCADE,
    purchased_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, book_id)
);

CREATE INDEX IF NOT EXISTS idx_user_purchases_user ON user_purchases (user_id);
