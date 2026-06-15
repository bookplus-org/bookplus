-- ============================================================
-- Order Service — V9: reclamos / disputas
-- ============================================================

ALTER TABLE orders
    ADD COLUMN claim_status     VARCHAR(20) NOT NULL DEFAULT 'NONE',
    ADD COLUMN claim_reason     VARCHAR(500),
    ADD COLUMN claim_resolution VARCHAR(500);

CREATE INDEX IF NOT EXISTS idx_orders_claim_status ON orders (claim_status);
