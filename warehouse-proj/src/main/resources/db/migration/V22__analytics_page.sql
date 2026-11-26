-- The Analytics page: give it to the standard roles that already exist (admin is topped up at startup, and on an
-- empty database startup creates the roles complete).
INSERT INTO role_pages (role_id, page_path)
SELECT r.id, '/analytics'
FROM roles r
WHERE r.is_system
  AND r.name IN ('manager', 'staff', 'viewer')
ON CONFLICT DO NOTHING;
