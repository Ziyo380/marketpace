CREATE TABLE cart_items (
    id UUID PRIMARY KEY,
    cart_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_cart_items_cart_product UNIQUE (cart_id, product_id),
    CONSTRAINT fk_cart_items_cart
        FOREIGN KEY (cart_id) REFERENCES carts (id),
    CONSTRAINT fk_cart_items_product
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_cart_items_quantity_positive
        CHECK (quantity > 0)
);
