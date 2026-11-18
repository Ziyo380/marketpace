SET client_encoding = 'UTF8';

CREATE EXTENSION IF NOT EXISTS pgcrypto;

BEGIN;

INSERT INTO users (id, username, password_hash, status, created_at, updated_at)
VALUES
    ('10000000-0000-0000-0000-000000000001', 'demo-admin', crypt('Admin123!', gen_salt('bf', 10)), 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000002', 'demo-seller', crypt('Seller123!', gen_salt('bf', 10)), 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000003', 'demo-customer', crypt('Customer123!', gen_salt('bf', 10)), 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000004', 'demo-applicant', crypt('Applicant123!', gen_salt('bf', 10)), 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (username) DO UPDATE
SET password_hash = EXCLUDED.password_hash,
    status = 'ACTIVE',
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'CUSTOMER'
WHERE u.username IN ('demo-admin', 'demo-seller', 'demo-customer', 'demo-applicant')
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ADMIN'
WHERE u.username = 'demo-admin'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'SELLER'
WHERE u.username = 'demo-seller'
ON CONFLICT DO NOTHING;

INSERT INTO sellers (id, user_id, status, approved_at, rejection_reason, created_at, updated_at)
SELECT
    '20000000-0000-0000-0000-000000000001',
    u.id,
    'APPROVED',
    CURRENT_TIMESTAMP,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users u
WHERE u.username = 'demo-seller'
ON CONFLICT (user_id) DO UPDATE
SET status = 'APPROVED',
    approved_at = CURRENT_TIMESTAMP,
    rejection_reason = NULL,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO sellers (id, user_id, status, approved_at, rejection_reason, created_at, updated_at)
SELECT
    '20000000-0000-0000-0000-000000000002',
    u.id,
    'PENDING',
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users u
WHERE u.username = 'demo-applicant'
ON CONFLICT (user_id) DO UPDATE
SET status = 'PENDING',
    approved_at = NULL,
    rejection_reason = NULL,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO stores (id, seller_id, name, slug, description, active, created_at, updated_at)
SELECT
    '30000000-0000-0000-0000-000000000001',
    s.id,
    'Nova Market',
    'nova-market',
    'Технологии, которыми приятно пользоваться каждый день.',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM sellers s
JOIN users u ON u.id = s.user_id
WHERE u.username = 'demo-seller'
ON CONFLICT (seller_id) DO UPDATE
SET name = EXCLUDED.name,
    slug = EXCLUDED.slug,
    description = EXCLUDED.description,
    active = TRUE,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO store_delivery_zones
    (id, store_id, region_code, delivery_fee, min_delivery_days, max_delivery_days, active, version, created_at, updated_at)
VALUES
    ('90000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001',
     'TASHKENT_CITY', 0.00, 1, 2, TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (store_id, region_code) DO UPDATE
SET delivery_fee = EXCLUDED.delivery_fee,
    min_delivery_days = EXCLUDED.min_delivery_days,
    max_delivery_days = EXCLUDED.max_delivery_days,
    active = TRUE,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO categories (id, name, slug, description, active, parent_id, created_at, updated_at)
VALUES
    ('40000000-0000-0000-0000-000000000001', 'Электроника', 'electronics', 'Смартфоны, аудио и умные устройства.', TRUE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('40000000-0000-0000-0000-000000000002', 'Для дома', 'home', 'Практичные и красивые вещи для дома.', TRUE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('40000000-0000-0000-0000-000000000003', 'Красота', 'beauty', 'Уход, ароматы и локальные бренды.', TRUE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('40000000-0000-0000-0000-000000000004', 'Спорт', 'sport', 'Товары для движения и активного отдыха.', TRUE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('40000000-0000-0000-0000-000000000005', 'Смартфоны', 'mobile-phones', 'Смартфоны и мобильные устройства.', TRUE, '40000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (slug) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    active = TRUE,
    parent_id = EXCLUDED.parent_id,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO products (id, store_id, category_id, name, slug, description, status, price, created_at, updated_at)
SELECT
    product.id,
    store.id,
    category.id,
    product.name,
    product.slug,
    product.description,
    'PUBLISHED',
    product.price,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM (
    VALUES
        ('50000000-0000-0000-0000-000000000001'::UUID, 'mobile-phones', 'Nova X1', 'nova-x1', 'Флагманский смартфон с ярким OLED-дисплеем и умной камерой.', 6490000.00),
        ('50000000-0000-0000-0000-000000000002'::UUID, 'electronics', 'Airbuds Flow', 'airbuds-flow', 'Лёгкие наушники с активным шумоподавлением на весь день.', 749000.00),
        ('50000000-0000-0000-0000-000000000003'::UUID, 'home', 'Luma Desk', 'luma-desk', 'Минималистичная настольная лампа с мягкой регулировкой света.', 389000.00),
        ('50000000-0000-0000-0000-000000000004'::UUID, 'beauty', 'Sahar No. 04', 'sahar-no-04', 'Тёплый древесный аромат с нотами инжира и белого чая.', 529000.00),
        ('50000000-0000-0000-0000-000000000005'::UUID, 'sport', 'Move Bottle', 'move-bottle', 'Термобутылка из стали для тренировок и поездок.', 199000.00),
        ('50000000-0000-0000-0000-000000000006'::UUID, 'electronics', 'Orbit Watch S', 'orbit-watch-s', 'Умные часы для спорта и повседневной жизни.', 1299000.00),
        ('50000000-0000-0000-0000-000000000007'::UUID, 'electronics', 'Studio Headphones', 'studio-headphones', 'Полноразмерные наушники с чистым звуком.', 1199000.00),
        ('50000000-0000-0000-0000-000000000008'::UUID, 'home', 'Mizu Ceramic Set', 'mizu-ceramic-set', 'Керамический набор для уютного дома.', 269000.00),
        ('50000000-0000-0000-0000-000000000009'::UUID, 'beauty', 'Aura Skin Serum', 'aura-skin-serum', 'Сыворотка для ежедневного ухода за кожей.', 319000.00),
        ('50000000-0000-0000-0000-000000000010'::UUID, 'sport', 'Flex Yoga Mat', 'flex-yoga-mat', 'Удобный коврик для йоги и тренировок.', 249000.00),
        ('50000000-0000-0000-0000-000000000011'::UUID, 'home', 'Halo Desk Lamp', 'halo-desk-lamp', 'Настольная лампа с мягким светом.', 459000.00),
        ('50000000-0000-0000-0000-000000000012'::UUID, 'electronics', 'Pulse Mini Speaker', 'pulse-mini-speaker', 'Компактная беспроводная колонка.', 439000.00)
) AS product(id, category_slug, name, slug, description, price)
JOIN stores store ON store.slug = 'nova-market'
JOIN categories category ON category.slug = product.category_slug
ON CONFLICT (store_id, slug) DO UPDATE
SET category_id = EXCLUDED.category_id,
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    status = 'PUBLISHED',
    price = EXCLUDED.price,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO inventories (id, product_id, quantity, reserved_quantity, version, created_at, updated_at)
SELECT gen_random_uuid(), product.id, 24, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM products product
JOIN stores store ON store.id = product.store_id
WHERE store.slug = 'nova-market'
  AND product.slug IN ('nova-x1', 'airbuds-flow', 'luma-desk', 'sahar-no-04', 'move-bottle',
                       'orbit-watch-s', 'studio-headphones', 'mizu-ceramic-set', 'aura-skin-serum',
                       'flex-yoga-mat', 'halo-desk-lamp', 'pulse-mini-speaker')
ON CONFLICT (product_id) DO NOTHING;

COMMIT;
