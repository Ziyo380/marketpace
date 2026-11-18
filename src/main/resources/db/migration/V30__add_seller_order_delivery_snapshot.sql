ALTER TABLE seller_orders
    ADD COLUMN delivery_fee NUMERIC(19,2),
    ADD COLUMN delivery_min_days INTEGER,
    ADD COLUMN delivery_max_days INTEGER,
    ADD CONSTRAINT chk_seller_orders_delivery_snapshot CHECK (
        (delivery_fee IS NULL AND delivery_min_days IS NULL AND delivery_max_days IS NULL)
        OR
        (delivery_fee IS NOT NULL AND delivery_fee >= 0
            AND delivery_min_days IS NOT NULL AND delivery_min_days >= 1
            AND delivery_max_days IS NOT NULL AND delivery_max_days >= delivery_min_days)
    );
