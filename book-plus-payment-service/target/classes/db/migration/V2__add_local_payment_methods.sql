-- ============================================================
-- Payment Service — V2: local payment methods (Yape, Plin, Card, Cash)
-- ============================================================
-- New enum values for the simulated local payment methods used by the
-- BookPlus checkout. ADD VALUE IF NOT EXISTS is idempotent (PostgreSQL 12+).

ALTER TYPE payment_method ADD VALUE IF NOT EXISTS 'YAPE';
ALTER TYPE payment_method ADD VALUE IF NOT EXISTS 'PLIN';
ALTER TYPE payment_method ADD VALUE IF NOT EXISTS 'CARD';
ALTER TYPE payment_method ADD VALUE IF NOT EXISTS 'CASH';
