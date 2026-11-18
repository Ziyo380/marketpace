CREATE TABLE sellers (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_at TIMESTAMP,
    rejection_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_sellers_user_id UNIQUE (user_id),
    CONSTRAINT fk_sellers_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_sellers_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED'))
);
