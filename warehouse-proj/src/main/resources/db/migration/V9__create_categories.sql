-- category 1..n products. Existing products stay uncategorised (category_id NULL).
CREATE TABLE categories (
    id           UUID         PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    description  VARCHAR(500),
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ,
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE
);

ALTER TABLE products ADD COLUMN category_id UUID REFERENCES categories (id);
CREATE INDEX idx_products_category_id ON products (category_id);
