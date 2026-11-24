CREATE TABLE inventory (
    id          UUID         PRIMARY KEY,
    product_id  UUID         NOT NULL REFERENCES products (id),
    status      VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_inventory_product_id ON inventory (product_id);
