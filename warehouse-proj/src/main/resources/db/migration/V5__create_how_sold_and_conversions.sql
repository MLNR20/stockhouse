CREATE TABLE how_sold (
    id          UUID         PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE
);

-- 1 parent unit = unit_conversion child units (e.g. 1 Case = 12 Each).
CREATE TABLE how_sold_conversions (
    id                    UUID          PRIMARY KEY,
    parent_how_sold_id    UUID          NOT NULL REFERENCES how_sold (id),
    child_how_sold_id     UUID          NOT NULL REFERENCES how_sold (id),
    unit_conversion       NUMERIC(19,4) NOT NULL CHECK (unit_conversion > 0),
    created_at            TIMESTAMPTZ   NOT NULL,
    updated_at            TIMESTAMPTZ,
    is_active             BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_how_sold_conversions_parent_ne_child CHECK (parent_how_sold_id <> child_how_sold_id)
);

CREATE INDEX idx_how_sold_conversions_parent ON how_sold_conversions (parent_how_sold_id);
CREATE INDEX idx_how_sold_conversions_child  ON how_sold_conversions (child_how_sold_id);
