-- Product name (200) + separator (3) + variant name (120).
ALTER TABLE order_items ALTER COLUMN product_name TYPE VARCHAR(323);
