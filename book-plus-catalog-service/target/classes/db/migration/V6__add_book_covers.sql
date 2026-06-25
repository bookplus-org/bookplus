-- ============================================================
-- Catalog Service — V6: book cover images (admin upload)
-- ============================================================
-- Permite subir la portada del libro como archivo. Se almacena en la BD y se
-- sirve por GET /api/v1/books/{id}/cover. Al subir, books.image_url se apunta
-- a ese endpoint.

CREATE TABLE IF NOT EXISTS book_covers (
    book_id      UUID         PRIMARY KEY REFERENCES books (id) ON DELETE CASCADE,
    image        BYTEA        NOT NULL,
    content_type VARCHAR(100) NOT NULL DEFAULT 'image/jpeg',
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
