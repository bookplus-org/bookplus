-- ============================================================
-- V2 — Datos iniciales: roles y usuario administrador
-- ============================================================

-- ── Roles ──────────────────────────────────────────────────
INSERT INTO roles (name) VALUES
    ('ROLE_USER'),
    ('ROLE_EDITOR'),
    ('ROLE_ADMIN'),
    ('ROLE_SUPERADMIN')
ON CONFLICT (name) DO NOTHING;

-- ── Usuario Admin por defecto ──────────────────────────────
-- Contraseña: Admin123! (BCrypt cost=12)
-- CAMBIAR EN PRODUCCIÓN via variable de entorno o script seguro.
INSERT INTO users (id, username, email, password_hash, enabled, email_verified)
VALUES (
    gen_random_uuid(),
    'admin',
    'admin@bookplus.com',
    '$2b$12$AoQN1x1E12ShF6oV16noF.1Q211SCApWEdttTYQ1nv5gms11aaWHW',
    TRUE,
    TRUE
) ON CONFLICT (username) DO NOTHING;

-- Asignar rol SUPERADMIN al admin
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM   users u, roles r
WHERE  u.username = 'admin'
AND    r.name = 'ROLE_SUPERADMIN'
ON CONFLICT DO NOTHING;
