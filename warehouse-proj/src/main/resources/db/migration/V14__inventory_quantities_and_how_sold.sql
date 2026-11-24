-- Inventory now records how much is on hand (in the base unit), the level that counts as running low,
-- and the unit it is sold in. Existing rows get 0 / 0 / not set.
ALTER TABLE inventory ADD COLUMN base_quantity      INTEGER NOT NULL DEFAULT 0 CHECK (base_quantity >= 0);
ALTER TABLE inventory ADD COLUMN minimum_threshold  INTEGER NOT NULL DEFAULT 0 CHECK (minimum_threshold >= 0);
ALTER TABLE inventory ADD COLUMN how_sold_id        UUID REFERENCES how_sold (id);
CREATE INDEX idx_inventory_how_sold_id ON inventory (how_sold_id);
