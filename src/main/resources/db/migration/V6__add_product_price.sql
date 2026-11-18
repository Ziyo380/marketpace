ALTER TABLE products
    ADD COLUMN price NUMERIC(19, 2);

ALTER TABLE products
    ADD CONSTRAINT chk_products_price_positive
        CHECK (price IS NULL OR price > 0);
