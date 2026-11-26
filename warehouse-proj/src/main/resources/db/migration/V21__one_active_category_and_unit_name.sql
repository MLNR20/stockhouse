-- At most one ACTIVE category, and one ACTIVE unit of measure, per name (ignoring case and spaces).
-- Soft-deleted rows do not count, so a deleted name can be used again.
--
-- Existing active duplicates are resolved first: the copy used the most is kept (the oldest if tied), anything
-- pointing at the other copies is moved to it, and the other copies are soft-deleted.

CREATE TEMPORARY TABLE category_duplicates ON COMMIT DROP AS
SELECT id, keeper_id
FROM (
    SELECT c.id,
           FIRST_VALUE(c.id) OVER (
               PARTITION BY LOWER(TRIM(c.name))
               ORDER BY (SELECT COUNT(*) FROM products p WHERE p.category_id = c.id) DESC, c.created_at, c.id
           ) AS keeper_id
    FROM categories c
    WHERE c.is_active
) ranked
WHERE id <> keeper_id;

UPDATE products p SET category_id = d.keeper_id, updated_at = NOW()
FROM category_duplicates d WHERE p.category_id = d.id;

UPDATE categories c SET is_active = FALSE, updated_at = NOW()
FROM category_duplicates d WHERE c.id = d.id;

CREATE TEMPORARY TABLE unit_duplicates ON COMMIT DROP AS
SELECT id, keeper_id
FROM (
    SELECT u.id,
           FIRST_VALUE(u.id) OVER (
               PARTITION BY LOWER(TRIM(u.name))
               ORDER BY ((SELECT COUNT(*) FROM inventory i WHERE i.how_sold_id = u.id)
                       + (SELECT COUNT(*) FROM order_details o WHERE o.how_sold_id = u.id)
                       + (SELECT COUNT(*) FROM reorders r WHERE r.how_sold_id = u.id)
                       + (SELECT COUNT(*) FROM how_sold_conversions v
                          WHERE v.parent_how_sold_id = u.id OR v.child_how_sold_id = u.id)) DESC,
                        u.created_at, u.id
           ) AS keeper_id
    FROM how_sold u
    WHERE u.is_active
) ranked
WHERE id <> keeper_id;

UPDATE inventory x SET how_sold_id = d.keeper_id, updated_at = NOW()
FROM unit_duplicates d WHERE x.how_sold_id = d.id;
UPDATE order_details x SET how_sold_id = d.keeper_id, updated_at = NOW()
FROM unit_duplicates d WHERE x.how_sold_id = d.id;
UPDATE reorders x SET how_sold_id = d.keeper_id, updated_at = NOW()
FROM unit_duplicates d WHERE x.how_sold_id = d.id;

-- A conversion between two copies of the same unit would become "X = n X": switch those off instead.
UPDATE how_sold_conversions v SET is_active = FALSE, updated_at = NOW()
FROM unit_duplicates d
WHERE (v.parent_how_sold_id = d.id AND v.child_how_sold_id = d.keeper_id)
   OR (v.child_how_sold_id = d.id AND v.parent_how_sold_id = d.keeper_id);
UPDATE how_sold_conversions v SET parent_how_sold_id = d.keeper_id, updated_at = NOW()
FROM unit_duplicates d WHERE v.parent_how_sold_id = d.id AND v.child_how_sold_id <> d.keeper_id;
UPDATE how_sold_conversions v SET child_how_sold_id = d.keeper_id, updated_at = NOW()
FROM unit_duplicates d WHERE v.child_how_sold_id = d.id AND v.parent_how_sold_id <> d.keeper_id;

UPDATE how_sold u SET is_active = FALSE, updated_at = NOW()
FROM unit_duplicates d WHERE u.id = d.id;

CREATE UNIQUE INDEX uq_categories_active_name ON categories (LOWER(TRIM(name))) WHERE is_active;
CREATE UNIQUE INDEX uq_how_sold_active_name ON how_sold (LOWER(TRIM(name))) WHERE is_active;
