CREATE TABLE customers (
    id            UUID         PRIMARY KEY,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(256) NOT NULL,
    phone_number  VARCHAR(30)  NOT NULL,
    address       VARCHAR(500) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE orders (
    id                    UUID          PRIMARY KEY,
    customer_id           UUID          NOT NULL REFERENCES customers (id),
    total_price           NUMERIC(19,2) NOT NULL CHECK (total_price >= 0),
    target_delivery_date  DATE          NOT NULL,
    status                VARCHAR(20)   NOT NULL,
    created_at            TIMESTAMPTZ   NOT NULL,
    updated_at            TIMESTAMPTZ,
    is_active             BOOLEAN       NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_orders_customer_id ON orders (customer_id);
