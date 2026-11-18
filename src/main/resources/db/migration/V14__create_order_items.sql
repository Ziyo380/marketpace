CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(19,2) NOT NULL,
    subtotal NUMERIC(19,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_order_items_order_product UNIQUE (order_id, product_id),
    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_items_product
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_order_items_quantity_positive
        CHECK (quantity > 0),
    CONSTRAINT chk_order_items_unit_price_non_negative
        CHECK (unit_price >= 0),
    CONSTRAINT chk_order_items_subtotal_non_negative
        CHECK (subtotal >= 0)
);
