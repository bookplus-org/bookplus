-- ============================================================
-- Order Service — V10: cupones de descuento
-- ============================================================

CREATE TABLE IF NOT EXISTS coupons (
    code           VARCHAR(40)   PRIMARY KEY,
    discount_type  VARCHAR(10)   NOT NULL,  -- PERCENT | FIXED
    discount_value NUMERIC(12,2) NOT NULL,
    min_amount     NUMERIC(12,2),
    active         BOOLEAN       NOT NULL DEFAULT TRUE,
    expires_at     TIMESTAMPTZ,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- Cupones de ejemplo
INSERT INTO coupons (code, discount_type, discount_value, min_amount) VALUES
    ('BIENVENIDO10', 'PERCENT', 10, NULL),
    ('LIBROS5',      'FIXED',    5, 20),
    ('SUPER20',      'PERCENT', 20, 50)
ON CONFLICT (code) DO NOTHING;

-- Cupón aplicado al pedido (snapshot)
ALTER TABLE orders
    ADD COLUMN coupon_code     VARCHAR(40),
    ADD COLUMN discount_amount NUMERIC(12,2) NOT NULL DEFAULT 0;
