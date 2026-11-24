-- supplier_products becomes a first-class entity (BaseEntity columns) so unlinking is a soft delete.
ALTER TABLE supplier_products ADD COLUMN id UUID;
UPDATE supplier_products SET id = gen_random_uuid();
ALTER TABLE supplier_products ALTER COLUMN id SET NOT NULL;

ALTER TABLE supplier_products DROP CONSTRAINT supplier_products_pkey;
ALTER TABLE supplier_products ADD PRIMARY KEY (id);
ALTER TABLE supplier_products ADD CONSTRAINT uq_supplier_products UNIQUE (supplier_id, product_id);

ALTER TABLE supplier_products ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE supplier_products ALTER COLUMN created_at DROP DEFAULT;
ALTER TABLE supplier_products ADD COLUMN updated_at TIMESTAMPTZ;
ALTER TABLE supplier_products ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;
