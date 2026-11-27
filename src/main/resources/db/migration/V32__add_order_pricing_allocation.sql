-- Historical seller orders have no reliable per-store discount allocation.
-- NULL in the new seller columns preserves that distinction.
ALTER TABLE seller_orders
    ADD COLUMN discount_amount NUMERIC(19,2),
    ADD COLUMN payable_amount NUMERIC(19,2),
    ADD CONSTRAINT chk_seller_orders_pricing_allocation CHECK (
        (discount_amount IS NULL AND payable_amount IS NULL)
        OR
        (discount_amount IS NOT NULL AND payable_amount IS NOT NULL
            AND discount_amount >= 0
            AND discount_amount <= total_amount
            AND payable_amount = total_amount + COALESCE(delivery_fee, 0) - discount_amount)
    );

ALTER TABLE orders
    ADD COLUMN current_total_amount NUMERIC(19,2),
    ADD CONSTRAINT chk_orders_current_total_amount CHECK (
        current_total_amount >= 0 AND current_total_amount <= total_amount
    );

-- A historical partial cancellation cannot be reconstructed without a
-- per-store discount allocation, so its current amount remains unknown.
UPDATE orders
SET current_total_amount = CASE
    WHEN status = 'CANCELLED' THEN 0
    WHEN status = 'PARTIALLY_CANCELLED' THEN NULL
    ELSE total_amount
END;
