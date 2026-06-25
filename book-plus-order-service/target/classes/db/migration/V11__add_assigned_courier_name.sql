-- ============================================================
-- Order Service — V11: nombre del repartidor asignado
-- ============================================================

ALTER TABLE orders
    ADD COLUMN assigned_courier_name VARCHAR(120);
