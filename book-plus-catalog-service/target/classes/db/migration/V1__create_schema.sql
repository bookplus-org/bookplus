-- ============================================================
-- Book+ Catalog Service — Schema V1
-- ============================================================

-- ── Categories ────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS categories (
    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    parent_id   UUID        REFERENCES categories(id) ON DELETE SET NULL,
    image_url   VARCHAR(512),
    active      BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_categories_name UNIQUE (name),
    CONSTRAINT uq_categories_slug UNIQUE (slug)
);

CREATE INDEX idx_categories_active    ON categories(active);
CREATE INDEX idx_categories_parent_id ON categories(parent_id);

-- ── Books ─────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS books (
    id              UUID            NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    isbn            VARCHAR(20)     NOT NULL,
    title           VARCHAR(255)    NOT NULL,
    slug            VARCHAR(300)    NOT NULL,
    author          VARCHAR(255)    NOT NULL,
    description     TEXT,
    price           NUMERIC(12, 2)  NOT NULL,
    currency        CHAR(3)         NOT NULL DEFAULT 'USD',
    discount_price  NUMERIC(12, 2),
    image_url       VARCHAR(512),
    publisher       VARCHAR(200),
    published_date  DATE,
    language        VARCHAR(10),
    pages           INTEGER,
    category_id     UUID            NOT NULL REFERENCES categories(id),
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    stock_snapshot  INTEGER         NOT NULL DEFAULT 0,
    average_rating  NUMERIC(3, 2)   NOT NULL DEFAULT 0.00,
    review_count    INTEGER         NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT uq_books_isbn UNIQUE (isbn),
    CONSTRAINT uq_books_slug UNIQUE (slug),
    CONSTRAINT chk_books_price_positive CHECK (price > 0),
    CONSTRAINT chk_books_discount_lt_price CHECK (discount_price IS NULL OR discount_price < price),
    CONSTRAINT chk_books_stock_non_negative CHECK (stock_snapshot >= 0),
    CONSTRAINT chk_books_rating_range CHECK (average_rating BETWEEN 0 AND 5),
    CONSTRAINT chk_books_review_count CHECK (review_count >= 0)
);

CREATE INDEX idx_books_isbn        ON books(isbn);
CREATE INDEX idx_books_slug        ON books(slug);
CREATE INDEX idx_books_category_id ON books(category_id);
CREATE INDEX idx_books_author      ON books(author);
CREATE INDEX idx_books_active      ON books(active);
CREATE INDEX idx_books_published   ON books(published_date DESC);
CREATE INDEX idx_books_rating      ON books(average_rating DESC);

-- ── Reviews ───────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS reviews (
    id                UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    book_id           UUID        NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    user_id           VARCHAR(100) NOT NULL,
    username          VARCHAR(100) NOT NULL,
    rating            SMALLINT    NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment           TEXT,
    verified_purchase BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_reviews_book_user UNIQUE (book_id, user_id)
);

CREATE INDEX idx_reviews_book_id    ON reviews(book_id);
CREATE INDEX idx_reviews_user_id    ON reviews(user_id);
CREATE INDEX idx_reviews_created_at ON reviews(created_at DESC);

-- ── Auto-update updated_at trigger ───────────────────────────────────────

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_categories_updated_at
    BEFORE UPDATE ON categories
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_books_updated_at
    BEFORE UPDATE ON books
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
