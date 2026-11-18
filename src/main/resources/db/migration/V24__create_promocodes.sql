CREATE TABLE promocodes (
    id UUID PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    discount_type VARCHAR(16) NOT NULL CHECK (discount_type IN ('PERCENT', 'FIXED')),
    discount_value NUMERIC(19,2) NOT NULL CHECK (discount_value > 0),
    minimum_amount NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK (minimum_amount >= 0),
    starts_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    max_uses INTEGER CHECK (max_uses > 0),
    used_count INTEGER NOT NULL DEFAULT 0 CHECK (used_count >= 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
ALTER TABLE orders ADD COLUMN promocode_id UUID REFERENCES promocodes(id);
ALTER TABLE orders ADD COLUMN discount_amount NUMERIC(19,2) NOT NULL DEFAULT 0
    CHECK (discount_amount >= 0);
ALTER TABLE orders ADD COLUMN subtotal_amount NUMERIC(19,2);
UPDATE orders SET subtotal_amount = total_amount;
ALTER TABLE orders ALTER COLUMN subtotal_amount SET NOT NULL;

CREATE TABLE promocode_usages (
    id UUID PRIMARY KEY,
    promocode_id UUID NOT NULL REFERENCES promocodes(id),
    user_id UUID NOT NULL REFERENCES users(id),
    order_id UUID NOT NULL UNIQUE REFERENCES orders(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_promocode_usages_user_code UNIQUE(user_id, promocode_id)
);
