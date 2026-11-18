CREATE TABLE product_variants (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id),
    sku VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    price NUMERIC(19,2) NOT NULL CHECK (price > 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    quantity INTEGER NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    reserved_quantity INTEGER NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_product_variants_reserve CHECK (reserved_quantity <= quantity)
);
CREATE INDEX idx_product_variants_product ON product_variants(product_id, active);

ALTER TABLE cart_items ADD COLUMN variant_id UUID REFERENCES product_variants(id);
ALTER TABLE cart_items DROP CONSTRAINT uk_cart_items_cart_product;
CREATE UNIQUE INDEX uk_cart_items_cart_product_variant
    ON cart_items(cart_id, product_id, variant_id) NULLS NOT DISTINCT;

ALTER TABLE order_items ADD COLUMN variant_id UUID REFERENCES product_variants(id);
ALTER TABLE order_items ADD COLUMN sku VARCHAR(80);
ALTER TABLE order_items DROP CONSTRAINT uk_order_items_order_product;
CREATE UNIQUE INDEX uk_order_items_order_product_variant
    ON order_items(order_id, product_id, variant_id) NULLS NOT DISTINCT;

ALTER TABLE stock_reservations ADD COLUMN variant_id UUID REFERENCES product_variants(id);
ALTER TABLE stock_reservations ALTER COLUMN inventory_id DROP NOT NULL;
ALTER TABLE stock_reservations ADD CONSTRAINT chk_stock_reservations_stock_source
    CHECK ((inventory_id IS NULL) <> (variant_id IS NULL));

ALTER TABLE inventory_movements ADD COLUMN variant_id UUID REFERENCES product_variants(id);
ALTER TABLE inventory_movements ALTER COLUMN inventory_id DROP NOT NULL;
ALTER TABLE inventory_movements ADD CONSTRAINT chk_inventory_movements_stock_source
    CHECK ((inventory_id IS NULL) <> (variant_id IS NULL));
CREATE INDEX idx_inventory_movements_variant_time
    ON inventory_movements(variant_id, created_at);
