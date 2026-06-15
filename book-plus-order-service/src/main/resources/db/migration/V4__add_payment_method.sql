-- ============================================================
-- Order Service — V4: payment method + wider country column
-- ============================================================

-- Chosen payment method snapshot (YAPE / PLIN / CARD / CASH)
ALTER TABLE orders
    ADD COLUMN payment_method VARCHAR(20) NOT NULL DEFAULT 'CARD';

-- Country can be a full name (e.g. 'Perú'), not just an ISO-3 code
ALTER TABLE orders
    ALTER COLUMN shipping_country TYPE VARCHAR(60);
