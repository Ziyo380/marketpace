CREATE TABLE orders (
    id UUID PRIMARY KEY,
    buyer_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    total_amount NUMERIC(19,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_orders_buyer
        FOREIGN KEY (buyer_id) REFERENCES users (id),
    CONSTRAINT chk_orders_status
        CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED')),
    CONSTRAINT chk_orders_total_amount_non_negative
        CHECK (total_amount >= 0)
);

CREATE INDEX idx_orders_buyer_id ON orders (buyer_id);
