-- ============================================================
-- Book+ Catalog Service — Seed Data V2
-- ============================================================

-- ── Categorías raíz ───────────────────────────────────────────────────────

INSERT INTO categories (id, name, slug, description, active)
VALUES
    ('11111111-0000-0000-0000-000000000001', 'Fiction',        'fiction',        'Novels, short stories, and literary fiction',        TRUE),
    ('11111111-0000-0000-0000-000000000002', 'Non-Fiction',    'non-fiction',    'Biographies, essays, journalism, and academic works', TRUE),
    ('11111111-0000-0000-0000-000000000003', 'Science',        'science',        'Physics, biology, mathematics, and technology',       TRUE),
    ('11111111-0000-0000-0000-000000000004', 'Technology',     'technology',     'Programming, software engineering, and IT',           TRUE),
    ('11111111-0000-0000-0000-000000000005', 'Business',       'business',       'Entrepreneurship, management, and economics',         TRUE),
    ('11111111-0000-0000-0000-000000000006', 'Self-Help',      'self-help',      'Personal development, mindfulness, and productivity', TRUE),
    ('11111111-0000-0000-0000-000000000007', 'History',        'history',        'World history, culture, and civilizations',           TRUE),
    ('11111111-0000-0000-0000-000000000008', 'Children',       'children',       'Picture books, early readers, and middle grade',      TRUE)
ON CONFLICT (name) DO NOTHING;

-- ── Sub-categorías de Technology ─────────────────────────────────────────

INSERT INTO categories (id, name, slug, description, parent_id, active)
VALUES
    ('11111111-0000-0000-0000-000000000009', 'Software Engineering', 'software-engineering',
     'Clean code, architecture, and design patterns',
     '11111111-0000-0000-0000-000000000004', TRUE),
    ('11111111-0000-0000-0000-000000000010', 'Data Science', 'data-science',
     'Machine learning, AI, and data analysis',
     '11111111-0000-0000-0000-000000000004', TRUE),
    ('11111111-0000-0000-0000-000000000011', 'DevOps & Cloud', 'devops-cloud',
     'CI/CD, Docker, Kubernetes, and cloud platforms',
     '11111111-0000-0000-0000-000000000004', TRUE)
ON CONFLICT (name) DO NOTHING;

-- ── Sample books ──────────────────────────────────────────────────────────

INSERT INTO books (id, isbn, title, slug, author, description, price, currency, category_id, active, stock_snapshot)
VALUES
    (
        '22222222-0000-0000-0000-000000000001',
        '9780132350884',
        'Clean Code: A Handbook of Agile Software Craftsmanship',
        'clean-code-a-handbook-of-agile-software-craftsmanship',
        'Robert C. Martin',
        'Even bad code can function. But if code isn''t clean, it can bring a development organization to its knees.',
        39.99, 'USD',
        '11111111-0000-0000-0000-000000000009',
        TRUE, 50
    ),
    (
        '22222222-0000-0000-0000-000000000002',
        '9780201633610',
        'Design Patterns: Elements of Reusable Object-Oriented Software',
        'design-patterns-elements-of-reusable-object-oriented-software',
        'Gang of Four',
        'Capturing solutions that have developed and evolved over time, this classic book describes 23 design patterns.',
        49.99, 'USD',
        '11111111-0000-0000-0000-000000000009',
        TRUE, 30
    ),
    (
        '22222222-0000-0000-0000-000000000003',
        '9780596517748',
        'JavaScript: The Good Parts',
        'javascript-the-good-parts',
        'Douglas Crockford',
        'Most programming languages contain good and bad parts, but JavaScript has more than its share of the bad.',
        29.99, 'USD',
        '11111111-0000-0000-0000-000000000004',
        TRUE, 75
    )
ON CONFLICT (isbn) DO NOTHING;
