-- ============================================================
-- Order Service — V12: prueba de entrega multimedia (foto + firma)
-- ============================================================

CREATE TABLE IF NOT EXISTS delivery_proofs (
    order_id              UUID PRIMARY KEY,
    photo                 BYTEA,
    photo_content_type    VARCHAR(60),
    signature             BYTEA,
    signature_content_type VARCHAR(60),
    received_by           VARCHAR(120),
    delivered_by          VARCHAR(120),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);
