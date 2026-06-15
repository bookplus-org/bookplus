-- ============================================================
-- Catalog Service — V7: PDF completo (solo-admin)
-- ============================================================
-- Además de la muestra pública (preview_pdf), guardamos el PDF completo para
-- que un administrador pueda consultarlo. Solo se sirve por un endpoint admin.

ALTER TABLE book_previews
    ADD COLUMN full_pdf   BYTEA,
    ADD COLUMN full_pages INTEGER;
