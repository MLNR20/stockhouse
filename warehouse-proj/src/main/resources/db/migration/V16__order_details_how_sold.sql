-- The unit an order line's quantity is counted in (2 Case, 10 Each...). NULL means the base unit.
ALTER TABLE order_details ADD COLUMN how_sold_id UUID REFERENCES how_sold (id);
CREATE INDEX idx_order_details_how_sold_id ON order_details (how_sold_id);
