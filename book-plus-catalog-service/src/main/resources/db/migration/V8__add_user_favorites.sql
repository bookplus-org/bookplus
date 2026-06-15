-- ============================================================
-- Catalog Service — V8: favoritos / lista de deseos
-- ============================================================

CREATE TABLE IF NOT EXISTS user_favorites (
    user_id    VARCHAR(255) NOT NULL,
    book_id    UUID         NOT NULL REFERENCES books (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, book_id)
);

CREATE INDEX IF NOT EXISTS idx_user_favorites_user ON user_favorites (user_id);
