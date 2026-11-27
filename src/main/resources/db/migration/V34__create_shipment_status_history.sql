CREATE TABLE shipment_status_history (
    id UUID PRIMARY KEY,
    shipment_id UUID NOT NULL,
    from_status VARCHAR(32),
    to_status VARCHAR(32) NOT NULL,
    actor_type VARCHAR(32) NOT NULL,
    actor_username VARCHAR(100),
    changed_at TIMESTAMPTZ NOT NULL,
    details VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_shipment_status_history_shipment
        FOREIGN KEY (shipment_id) REFERENCES shipments (id),
    CONSTRAINT chk_shipment_status_history_from_status
        CHECK (from_status IN (
            'CREATED', 'PACKING', 'READY_FOR_DISPATCH',
            'IN_TRANSIT', 'DELIVERED', 'DELIVERY_FAILED'
        )),
    CONSTRAINT chk_shipment_status_history_to_status
        CHECK (to_status IN (
            'CREATED', 'PACKING', 'READY_FOR_DISPATCH',
            'IN_TRANSIT', 'DELIVERED', 'DELIVERY_FAILED'
        )),
    CONSTRAINT chk_shipment_status_history_actor_type
        CHECK (actor_type IN ('SELLER', 'SYSTEM', 'ADMIN')),
    CONSTRAINT chk_shipment_status_history_initial_event
        CHECK (
            (from_status IS NULL AND to_status = 'CREATED')
            OR (from_status IS NOT NULL AND to_status <> 'CREATED')
        )
);

CREATE INDEX idx_shipment_status_history_shipment_time_id
    ON shipment_status_history (shipment_id, changed_at, id);
