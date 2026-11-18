ALTER TABLE orders ADD COLUMN idempotency_key VARCHAR(128);
ALTER TABLE orders ADD COLUMN expires_at TIMESTAMPTZ;
CREATE UNIQUE INDEX uk_orders_buyer_idempotency_key
    ON orders (buyer_id, idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX idx_orders_expiry ON orders (expires_at)
    WHERE expires_at IS NOT NULL;

-- Older orders were created before the seller-order model existed.
INSERT INTO seller_orders (id, order_id, store_id, status, total_amount,
                           confirmed_at, cancelled_at, cancellation_reason,
                           version, created_at, updated_at)
SELECT gen_random_uuid(), oi.order_id, p.store_id,
       CASE WHEN o.status = 'CANCELLED' THEN 'CANCELLED'
            WHEN o.status = 'CONFIRMED' THEN 'CONFIRMED' ELSE 'PENDING' END,
       SUM(oi.subtotal),
       CASE WHEN o.status = 'CONFIRMED' THEN o.updated_at END,
       CASE WHEN o.status = 'CANCELLED' THEN o.updated_at END,
       CASE WHEN o.status = 'CANCELLED' THEN 'Cancelled before seller-order migration' END,
       0, MIN(oi.created_at), CURRENT_TIMESTAMP
FROM order_items oi
JOIN products p ON p.id = oi.product_id
JOIN orders o ON o.id = oi.order_id
WHERE NOT EXISTS (SELECT 1 FROM seller_orders so
                  WHERE so.order_id = oi.order_id AND so.store_id = p.store_id)
GROUP BY oi.order_id, p.store_id, o.status, o.updated_at;

UPDATE order_items oi
SET seller_order_id = so.id,
    product_name = p.name
FROM products p, seller_orders so
WHERE oi.product_id = p.id
  AND so.order_id = oi.order_id AND so.store_id = p.store_id
  AND (oi.seller_order_id IS NULL OR oi.product_name IS NULL);

-- Repair orders cancelled by the old buyer path, which left seller parts open.
UPDATE seller_orders so
SET status = 'CANCELLED', cancelled_at = COALESCE(so.cancelled_at, CURRENT_TIMESTAMP),
    cancellation_reason = COALESCE(so.cancellation_reason, 'Buyer cancelled order'),
    updated_at = CURRENT_TIMESTAMP, version = so.version + 1
FROM orders o
WHERE so.order_id = o.id AND o.status = 'CANCELLED' AND so.status = 'PENDING';

UPDATE stock_reservations sr
SET status = 'RELEASED', closed_at = COALESCE(sr.closed_at, CURRENT_TIMESTAMP),
    updated_at = CURRENT_TIMESTAMP, version = sr.version + 1
FROM order_items oi JOIN orders o ON o.id = oi.order_id
WHERE sr.order_item_id = oi.id AND o.status = 'CANCELLED' AND sr.status = 'ACTIVE';

UPDATE stock_reservations sr
SET status = 'CONFIRMED', closed_at = COALESCE(sr.closed_at, CURRENT_TIMESTAMP),
    updated_at = CURRENT_TIMESTAMP, version = sr.version + 1
FROM order_items oi JOIN seller_orders so ON so.id = oi.seller_order_id
WHERE sr.order_item_id = oi.id AND so.status = 'CONFIRMED' AND sr.status = 'ACTIVE';

-- Legacy pending order items had stock reserved but no reservation record yet.
INSERT INTO stock_reservations (id, inventory_id, order_item_id, quantity, status,
                                closed_at, version, created_at, updated_at)
SELECT gen_random_uuid(), i.id, oi.id, oi.quantity, 'ACTIVE',
       NULL, 0, oi.created_at, CURRENT_TIMESTAMP
FROM order_items oi
JOIN seller_orders so ON so.id = oi.seller_order_id
JOIN inventories i ON i.product_id = oi.product_id
WHERE so.status = 'PENDING'
  AND NOT EXISTS (SELECT 1 FROM stock_reservations sr WHERE sr.order_item_id = oi.id);

-- Reservation records are the source of truth for stock still held.
UPDATE inventories i
SET reserved_quantity = COALESCE((SELECT SUM(sr.quantity)
                                  FROM stock_reservations sr
                                  WHERE sr.inventory_id = i.id AND sr.status = 'ACTIVE'), 0),
    updated_at = CURRENT_TIMESTAMP, version = i.version + 1
WHERE i.reserved_quantity <> COALESCE((SELECT SUM(sr.quantity)
                                       FROM stock_reservations sr
                                       WHERE sr.inventory_id = i.id AND sr.status = 'ACTIVE'), 0);

ALTER TABLE order_items ALTER COLUMN seller_order_id SET NOT NULL;
ALTER TABLE order_items ALTER COLUMN product_name SET NOT NULL;
