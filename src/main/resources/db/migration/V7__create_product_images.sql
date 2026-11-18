CREATE TABLE product_images (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    object_key VARCHAR(512) NOT NULL UNIQUE,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_product_images_product
        FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT uk_product_images_product_sort
        UNIQUE (product_id, sort_order),
    CONSTRAINT chk_product_images_size
        CHECK (size_bytes > 0),
    CONSTRAINT chk_product_images_sort_order
        CHECK (sort_order >= 0)
);

CREATE INDEX idx_product_images_product_id
    ON product_images (product_id);
