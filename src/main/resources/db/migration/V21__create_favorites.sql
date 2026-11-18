CREATE TABLE favorites (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    product_id UUID NOT NULL REFERENCES products(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_favorites_user_product UNIQUE (user_id, product_id)
);
CREATE INDEX idx_favorites_user_created ON favorites(user_id, created_at DESC);
