CREATE TABLE store_delivery_zones (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL,
    region_code VARCHAR(32) NOT NULL,
    delivery_fee NUMERIC(19,2) NOT NULL,
    min_delivery_days INTEGER NOT NULL,
    max_delivery_days INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_store_delivery_zones_store
        FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_store_delivery_zones_region
        FOREIGN KEY (region_code) REFERENCES delivery_regions (code),
    CONSTRAINT uk_store_delivery_zones_store_region
        UNIQUE (store_id, region_code),
    CONSTRAINT chk_store_delivery_zones_fee_non_negative
        CHECK (delivery_fee >= 0),
    CONSTRAINT chk_store_delivery_zones_min_days_positive
        CHECK (min_delivery_days >= 1),
    CONSTRAINT chk_store_delivery_zones_max_days_not_below_min
        CHECK (max_delivery_days >= min_delivery_days)
);

-- The UNIQUE constraint already indexes store_id as its first column.
CREATE INDEX idx_store_delivery_zones_region_code
    ON store_delivery_zones (region_code);
