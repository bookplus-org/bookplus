-- ============================================================
-- Order Service — V13: email del comprador (para notificaciones)
-- ============================================================

ALTER TABLE orders ADD COLUMN user_email VARCHAR(160);
