ALTER TABLE orders
    ADD COLUMN delivery_total_amount NUMERIC(19,2) NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_orders_delivery_total_amount_non_negative
        CHECK (delivery_total_amount >= 0);
