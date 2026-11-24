-- order 1..n shipments, truck 1..n shipments, warehouse 1..n employees, employee 1..n shipments (optional)
CREATE TABLE trucks (
    id            UUID          PRIMARY KEY,
    plate_number  VARCHAR(20)   NOT NULL,
    model         VARCHAR(100)  NOT NULL,
    capacity_kg   NUMERIC(10,2) NOT NULL CHECK (capacity_kg > 0),
    status        VARCHAR(20)   NOT NULL,
    created_at    TIMESTAMPTZ   NOT NULL,
    updated_at    TIMESTAMPTZ,
    is_active     BOOLEAN       NOT NULL DEFAULT TRUE
);

CREATE TABLE warehouse_employees (
    id            UUID          PRIMARY KEY,
    warehouse_id  UUID          NOT NULL REFERENCES warehouses (id),
    first_name    VARCHAR(100)  NOT NULL,
    last_name     VARCHAR(100)  NOT NULL,
    email         VARCHAR(256)  NOT NULL,
    phone_number  VARCHAR(30)   NOT NULL,
    created_at    TIMESTAMPTZ   NOT NULL,
    updated_at    TIMESTAMPTZ,
    is_active     BOOLEAN       NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_warehouse_employees_warehouse_id ON warehouse_employees (warehouse_id);

CREATE TABLE shipments (
    id              UUID         PRIMARY KEY,
    order_id        UUID         NOT NULL REFERENCES orders (id),
    truck_id        UUID         NOT NULL REFERENCES trucks (id),
    employee_id     UUID         REFERENCES warehouse_employees (id),
    scheduled_date  DATE         NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_shipments_order_id    ON shipments (order_id);
CREATE INDEX idx_shipments_truck_id    ON shipments (truck_id);
CREATE INDEX idx_shipments_employee_id ON shipments (employee_id);
