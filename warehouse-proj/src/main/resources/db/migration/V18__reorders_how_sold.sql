-- The unit a reorder's quantity is counted in (Each, Case, Pallet...). Optional.
ALTER TABLE reorders ADD COLUMN how_sold_id UUID REFERENCES how_sold (id);
CREATE INDEX idx_reorders_how_sold_id ON reorders (how_sold_id);
