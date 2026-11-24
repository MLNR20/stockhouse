-- A unit conversion can belong to one product (1 Case of Wireless Mouse = 12 Each). Existing conversions
-- keep product_id NULL, meaning they apply to every product.
ALTER TABLE how_sold_conversions ADD COLUMN product_id UUID REFERENCES products (id);
CREATE INDEX idx_how_sold_conversions_product ON how_sold_conversions (product_id);
