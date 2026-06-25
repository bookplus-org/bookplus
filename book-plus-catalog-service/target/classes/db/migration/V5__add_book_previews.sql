-- ============================================================
-- Catalog Service — V5: book PDF previews (sample = first N pages)
-- ============================================================
-- El PDF completo que sube el admin NO se almacena; solo se guarda la
-- muestra generada con las primeras N páginas, que es lo único que se sirve
-- públicamente en el visor.

CREATE TABLE IF NOT EXISTS book_previews (
    book_id      UUID         PRIMARY KEY REFERENCES books (id) ON DELETE CASCADE,
    preview_pdf  BYTEA        NOT NULL,
    page_count   INTEGER      NOT NULL,
    source_pages INTEGER,
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
