CREATE TABLE stores (
    id UUID PRIMARY KEY,
    seller_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(180) NOT NULL,
    description VARCHAR(1000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_stores_seller_id UNIQUE (seller_id),
    CONSTRAINT uk_stores_slug UNIQUE (slug),
    CONSTRAINT fk_stores_seller
        FOREIGN KEY (seller_id) REFERENCES sellers (id)
);
