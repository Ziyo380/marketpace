CREATE TABLE carts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_carts_user UNIQUE (user_id),
    CONSTRAINT fk_carts_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);
