ALTER TABLE orders
    ADD COLUMN recipient_name VARCHAR(100),
    ADD COLUMN recipient_phone VARCHAR(30),
    ADD COLUMN delivery_region VARCHAR(100),
    ADD COLUMN delivery_city VARCHAR(100),
    ADD COLUMN delivery_address_line VARCHAR(500);

-- Existing orders predate checkout delivery details and remain readable.
-- New checkouts require all five fields through the API and service.
