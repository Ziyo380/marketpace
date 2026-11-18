CREATE TABLE stock_reservations (
    id UUID PRIMARY KEY,
    inventory_id UUID NOT NULL,
    order_item_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    closed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_stock_reservations_order_item UNIQUE (order_item_id),
    CONSTRAINT fk_stock_reservations_inventory
        FOREIGN KEY (inventory_id) REFERENCES inventories (id),
    CONSTRAINT fk_stock_reservations_order_item
        FOREIGN KEY (order_item_id) REFERENCES order_items (id),
    CONSTRAINT chk_stock_reservations_quantity_positive
        CHECK (quantity > 0),
    CONSTRAINT chk_stock_reservations_status
        CHECK (status IN ('ACTIVE', 'CONFIRMED', 'RELEASED'))
);

CREATE INDEX idx_stock_reservations_inventory_status
    ON stock_reservations (inventory_id, status);
