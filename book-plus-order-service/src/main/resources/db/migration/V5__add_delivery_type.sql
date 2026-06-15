-- ============================================================
-- Order Service — V5: tipo de entrega (digital / físico)
-- ============================================================

ALTER TABLE orders
    ADD COLUMN delivery_type VARCHAR(20) NOT NULL DEFAULT 'PHYSICAL';
