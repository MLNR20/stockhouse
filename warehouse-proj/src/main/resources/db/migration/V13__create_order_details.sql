-- The line items of an order: which products, how many, and at what sales price.
CREATE TABLE order_details (
    id           UUID          PRIMARY KEY,
    order_id     UUID          NOT NULL REFERENCES orders (id),
    product_id   UUID          NOT NULL REFERENCES products (id),
    quantity     INTEGER       NOT NULL CHECK (quantity > 0),
    sales_price  NUMERIC(19,2) NOT NULL CHECK (sales_price >= 0),
    created_at   TIMESTAMPTZ   NOT NULL,
    updated_at   TIMESTAMPTZ,
    is_active    BOOLEAN       NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_order_details_order_id   ON order_details (order_id);
CREATE INDEX idx_order_details_product_id ON order_details (product_id);
