CREATE TABLE order_status_history (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id),
    seller_order_id UUID REFERENCES seller_orders(id),
    previous_status VARCHAR(32),
    new_status VARCHAR(32) NOT NULL,
    actor VARCHAR(32) NOT NULL,
    reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_order_status_history_order_time ON order_status_history(order_id, created_at);

CREATE TABLE inventory_movements (
    id UUID PRIMARY KEY,
    inventory_id UUID NOT NULL REFERENCES inventories(id),
    type VARCHAR(32) NOT NULL,
    quantity_delta INTEGER NOT NULL,
    reserved_delta INTEGER NOT NULL,
    quantity_after INTEGER NOT NULL,
    reserved_after INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_inventory_movements_type CHECK
        (type IN ('BASELINE', 'STOCK_ADDED', 'STOCK_REMOVED', 'RESERVED', 'RELEASED', 'CONFIRMED'))
);
CREATE INDEX idx_inventory_movements_inventory_time ON inventory_movements(inventory_id, created_at);

INSERT INTO order_status_history(id, order_id, seller_order_id, previous_status,
                                 new_status, actor, reason, created_at, updated_at)
SELECT gen_random_uuid(), id, NULL, NULL, status, 'SYSTEM',
       'State at history migration', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM orders;
INSERT INTO order_status_history(id, order_id, seller_order_id, previous_status,
                                 new_status, actor, reason, created_at, updated_at)
SELECT gen_random_uuid(), order_id, id, NULL, status, 'SYSTEM',
       'State at history migration', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM seller_orders;
INSERT INTO inventory_movements(id, inventory_id, type, quantity_delta,
                                reserved_delta, quantity_after, reserved_after, created_at, updated_at)
SELECT gen_random_uuid(), id, 'BASELINE', quantity, reserved_quantity,
       quantity, reserved_quantity, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM inventories;
