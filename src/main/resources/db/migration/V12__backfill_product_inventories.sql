INSERT INTO inventories (
    id,
    product_id,
    quantity,
    reserved_quantity,
    version,
    created_at,
    updated_at
)
SELECT
    product.id,
    product.id,
    0,
    0,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM products product
WHERE NOT EXISTS (
    SELECT 1
    FROM inventories inventory
    WHERE inventory.product_id = product.id
);
