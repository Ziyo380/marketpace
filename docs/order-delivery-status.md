# Order and delivery status

SellerOrder retains its commercial state (PENDING, CONFIRMED or CANCELLED).
Shipment retains the delivery state. Order.status combines the current state
of all seller parts and their shipments.

Rules are evaluated in this order:

| Condition | Order status |
| --- | --- |
| Every part cancelled | CANCELLED |
| An active part has a failed delivery | DELIVERY_FAILED |
| Every non-cancelled part confirmed and delivered | DELIVERED |
| Some active parts delivered | PARTIALLY_DELIVERED |
| An active part in transit | IN_DELIVERY |
| Some parts cancelled | PARTIALLY_CANCELLED |
| Both pending and confirmed parts | PROCESSING |
| Only pending parts | PENDING |
| Remaining confirmed parts awaiting dispatch | CONFIRMED |

Missing shipments never count as delivered. Cancelled parts are excluded from
delivery completion. Their cancellations remain visible in seller parts and
order history, including when the remaining parts are delivered. A failed
delivery requires attention; it does not cancel the purchase, release already
consumed stock or imply a refund.

All mutations lock the parent Order first, before reading or modifying its
parts and shipments. The shipment, shipment history, aggregate Order status and
order history commit in the same transaction. Expiration selects orders with
pending parts regardless of their aggregate status; it only releases pending
reservations and then runs the same status calculation.

V35 recalculates existing orders from their current parts and shipments. If an
order changes, it records a SYSTEM event at migration time. It does not create
fictional past shipment events.
