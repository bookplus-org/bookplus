-- ============================================================
-- Order Service — V7: prueba de entrega (código + receptor)
-- ============================================================

ALTER TABLE orders
    ADD COLUMN delivery_code VARCHAR(12),
    ADD COLUMN received_by   VARCHAR(120);
