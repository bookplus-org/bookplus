-- ============================================================
-- Auth Service — V3: tokens de verificación de correo
-- ============================================================

CREATE TABLE IF NOT EXISTS email_verification_tokens (
    token      VARCHAR(80)  PRIMARY KEY,
    user_id    UUID         NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_email_verif_user ON email_verification_tokens (user_id);
