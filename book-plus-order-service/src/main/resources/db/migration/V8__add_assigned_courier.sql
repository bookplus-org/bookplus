-- ============================================================
-- Order Service — V8: repartidor asignado (autoasignación)
-- ============================================================

ALTER TABLE orders
    ADD COLUMN assigned_courier VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_orders_assigned_courier ON orders (assigned_courier);
