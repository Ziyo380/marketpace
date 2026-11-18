CREATE TABLE seller_orders (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    store_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    total_amount NUMERIC(19,2) NOT NULL,
    confirmed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    cancellation_reason TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_seller_orders_order_store UNIQUE (order_id, store_id),
    CONSTRAINT fk_seller_orders_order
        FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_seller_orders_store
        FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT chk_seller_orders_status
        CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED')),
    CONSTRAINT chk_seller_orders_total_amount_non_negative
        CHECK (total_amount >= 0)
);

CREATE INDEX idx_seller_orders_store_status
    ON seller_orders (store_id, status);

ALTER TABLE orders
    DROP CONSTRAINT IF EXISTS chk_orders_status;

ALTER TABLE orders
    ADD CONSTRAINT chk_orders_status
        CHECK (status IN (
            'PENDING',
            'PROCESSING',
            'CONFIRMED',
            'PARTIALLY_CANCELLED',
            'CANCELLED'
        ));

ALTER TABLE order_items
    ADD COLUMN seller_order_id UUID;

ALTER TABLE order_items
    ADD COLUMN product_name VARCHAR(255);

ALTER TABLE order_items
    ADD CONSTRAINT fk_order_items_seller_order
        FOREIGN KEY (seller_order_id) REFERENCES seller_orders (id);
