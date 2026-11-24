-- warehouse 1..n aisles 1..n shelves 1..n rows 1..n containers n..n products (with quantity)
CREATE TABLE warehouses (
    id          UUID         PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE aisles (
    id            UUID         PRIMARY KEY,
    warehouse_id  UUID         NOT NULL REFERENCES warehouses (id),
    name          VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_aisles_warehouse_id ON aisles (warehouse_id);

CREATE TABLE shelves (
    id          UUID         PRIMARY KEY,
    aisle_id    UUID         NOT NULL REFERENCES aisles (id),
    name        VARCHAR(100) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_shelves_aisle_id ON shelves (aisle_id);

CREATE TABLE shelf_rows (
    id          UUID         PRIMARY KEY,
    shelf_id    UUID         NOT NULL REFERENCES shelves (id),
    name        VARCHAR(100) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_shelf_rows_shelf_id ON shelf_rows (shelf_id);

CREATE TABLE containers (
    id            UUID         PRIMARY KEY,
    shelf_row_id  UUID         NOT NULL REFERENCES shelf_rows (id),
    name          VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_containers_shelf_row_id ON containers (shelf_row_id);

-- Contents of a container. Rows are hard-deleted when a product is removed from a container.
CREATE TABLE container_products (
    id            UUID         PRIMARY KEY,
    container_id  UUID         NOT NULL REFERENCES containers (id),
    product_id    UUID         NOT NULL REFERENCES products (id),
    quantity      INTEGER      NOT NULL CHECK (quantity > 0),
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_container_products UNIQUE (container_id, product_id)
);
CREATE INDEX idx_container_products_product_id ON container_products (product_id);
