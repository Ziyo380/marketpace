CREATE TABLE reviews (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    product_id UUID NOT NULL REFERENCES products(id),
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment VARCHAR(2000),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_reviews_user_product UNIQUE(user_id, product_id)
);
CREATE INDEX idx_reviews_product_status ON reviews(product_id, status, created_at DESC);
