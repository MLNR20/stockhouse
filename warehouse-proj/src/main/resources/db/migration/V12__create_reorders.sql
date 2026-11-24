-- A reorder asks a supplier for more of a product. supplier 1..n reorders, product 1..n reorders.
CREATE TABLE reorders (
    id            UUID         PRIMARY KEY,
    supplier_id   UUID         NOT NULL REFERENCES suppliers (id),
    product_id    UUID         NOT NULL REFERENCES products (id),
    quantity      INTEGER      NOT NULL CHECK (quantity > 0),
    expected_date DATE,
    status        VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_reorders_supplier_id ON reorders (supplier_id);
CREATE INDEX idx_reorders_product_id  ON reorders (product_id);
