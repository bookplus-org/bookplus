-- ============================================================
-- Order Service — V6: datos de envío (paquetería + tracking)
-- ============================================================

ALTER TABLE orders
    ADD COLUMN carrier         VARCHAR(80),
    ADD COLUMN tracking_number VARCHAR(120);
