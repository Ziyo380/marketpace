CREATE TABLE inventories (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 0,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_inventories_product UNIQUE (product_id),
    CONSTRAINT fk_inventories_product
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_inventories_quantity_non_negative
        CHECK (quantity >= 0),
    CONSTRAINT chk_inventories_reserved_non_negative
        CHECK (reserved_quantity >= 0),
    CONSTRAINT chk_inventories_reserved_not_above_quantity
        CHECK (reserved_quantity <= quantity)
);
