ALTER TABLE orders DROP CONSTRAINT IF EXISTS chk_orders_status;
ALTER TABLE orders ADD CONSTRAINT chk_orders_status CHECK (status IN (
    'PENDING', 'PROCESSING', 'CONFIRMED', 'PARTIALLY_CANCELLED', 'CANCELLED',
    'IN_DELIVERY', 'PARTIALLY_DELIVERED', 'DELIVERED', 'DELIVERY_FAILED'
));

-- Synchronize the current state of existing orders, without inventing past events.
WITH summary AS (
    SELECT so.order_id,
           COUNT(*) AS total,
           COUNT(*) FILTER (WHERE so.status <> 'CANCELLED') AS active,
           COUNT(*) FILTER (WHERE so.status = 'PENDING') AS pending,
           COUNT(*) FILTER (WHERE so.status = 'CONFIRMED') AS confirmed,
           COUNT(*) FILTER (WHERE so.status <> 'CANCELLED' AND s.status = 'DELIVERY_FAILED') AS failed,
           COUNT(*) FILTER (WHERE so.status = 'CONFIRMED' AND s.status = 'DELIVERED') AS delivered,
           COUNT(*) FILTER (WHERE so.status <> 'CANCELLED' AND s.status = 'IN_TRANSIT') AS in_transit
    FROM seller_orders so
    LEFT JOIN shipments s ON s.seller_order_id = so.id
    GROUP BY so.order_id
), targets AS MATERIALIZED (
    SELECT o.id, o.status AS previous_status,
           CASE
               WHEN summary.active = 0 THEN 'CANCELLED'
               WHEN summary.failed > 0 THEN 'DELIVERY_FAILED'
               WHEN summary.delivered = summary.active THEN 'DELIVERED'
               WHEN summary.delivered > 0 THEN 'PARTIALLY_DELIVERED'
               WHEN summary.in_transit > 0 THEN 'IN_DELIVERY'
               WHEN summary.active < summary.total THEN 'PARTIALLY_CANCELLED'
               WHEN summary.pending > 0 AND summary.confirmed > 0 THEN 'PROCESSING'
               WHEN summary.pending > 0 THEN 'PENDING'
               ELSE 'CONFIRMED'
           END AS next_status
    FROM orders o
    JOIN summary ON summary.order_id = o.id
), updated AS (
    UPDATE orders o
    SET status = targets.next_status,
        version = o.version + 1,
        updated_at = CURRENT_TIMESTAMP
    FROM targets
    WHERE o.id = targets.id AND o.status <> targets.next_status
    RETURNING o.id, targets.previous_status, o.status
)
INSERT INTO order_status_history
    (id, order_id, seller_order_id, previous_status, new_status, actor, reason, created_at, updated_at)
SELECT gen_random_uuid(), id, NULL, previous_status, status, 'SYSTEM',
       'Order synchronized with current shipments', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM updated;
