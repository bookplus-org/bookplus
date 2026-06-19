-- ============================================================
-- Cifrado de PII en reposo — ampliar columnas a TEXT
-- ============================================================
-- shipping_recipient_name y shipping_street pasan a guardarse cifradas (AES-GCM, Base64),
-- lo que ocupa más que el texto plano. Se amplían a TEXT en la tabla principal y en la de
-- auditoría de Envers (orders_aud) para que quepa el ciphertext. El cifrado/descifrado lo
-- hace de forma transparente CryptoConverter (@Convert) en la capa JPA.

ALTER TABLE orders     ALTER COLUMN shipping_recipient_name TYPE TEXT;
ALTER TABLE orders     ALTER COLUMN shipping_street         TYPE TEXT;
ALTER TABLE orders_aud ALTER COLUMN shipping_recipient_name TYPE TEXT;
ALTER TABLE orders_aud ALTER COLUMN shipping_street         TYPE TEXT;
