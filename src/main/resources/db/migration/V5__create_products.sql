CREATE TABLE products (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL,
    category_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    slug VARCHAR(220) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_products_store_slug UNIQUE (store_id, slug),
    CONSTRAINT chk_products_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT fk_products_store
        FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_products_category
        FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE INDEX idx_products_category_id ON products (category_id);
