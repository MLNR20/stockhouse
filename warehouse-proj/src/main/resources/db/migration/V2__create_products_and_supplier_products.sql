CREATE TABLE products (
    id          UUID          PRIMARY KEY,
    name        VARCHAR(200)  NOT NULL,
    base_price  NUMERIC(19,2) NOT NULL CHECK (base_price >= 0),
    created_at  TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ,
    is_active   BOOLEAN       NOT NULL DEFAULT TRUE
);

CREATE TABLE supplier_products (
    supplier_id  UUID NOT NULL REFERENCES suppliers (id) ON DELETE CASCADE,
    product_id   UUID NOT NULL REFERENCES products (id)  ON DELETE CASCADE,
    PRIMARY KEY (supplier_id, product_id)
);

CREATE INDEX idx_supplier_products_product_id ON supplier_products (product_id);
