CREATE TABLE shipments (
    id UUID PRIMARY KEY,
    seller_order_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    carrier_name VARCHAR(100),
    tracking_number VARCHAR(128),
    shipped_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    failure_reason VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_shipments_seller_order UNIQUE (seller_order_id),
    CONSTRAINT fk_shipments_seller_order
        FOREIGN KEY (seller_order_id) REFERENCES seller_orders (id),
    CONSTRAINT chk_shipments_status
        CHECK (status IN (
            'CREATED',
            'PACKING',
            'READY_FOR_DISPATCH',
            'IN_TRANSIT',
            'DELIVERED',
            'DELIVERY_FAILED'
        ))
);

CREATE INDEX idx_shipments_status ON shipments (status);
