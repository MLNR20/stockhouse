-- Deleting a supplier used to switch off its product links, which hid the supplier from those products.
-- Deleting now leaves the links alone (and products list the supplier as "deleted"), so bring back the links
-- that earlier deletions switched off. Only links to products that still exist are restored.
UPDATE supplier_products sp
SET is_active = TRUE,
    updated_at = NOW()
FROM suppliers s, products p
WHERE sp.supplier_id = s.id
  AND s.is_active = FALSE
  AND sp.product_id = p.id
  AND p.is_active = TRUE
  AND sp.is_active = FALSE;
