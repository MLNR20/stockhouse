-- Forgot password: only a hash of the single-use token is stored.
ALTER TABLE app_users ADD COLUMN reset_token_hash       VARCHAR(64);
ALTER TABLE app_users ADD COLUMN reset_token_expires_at TIMESTAMPTZ;
CREATE INDEX idx_app_users_reset_token ON app_users (reset_token_hash);

-- Give the standard roles what the catalog gives them by default for areas added after they were first created
-- (warehouses, shelves, customers and the new pages). The admin role is topped up at startup. On an empty
-- database there are no roles yet, so these insert nothing and startup creates the roles complete.
INSERT INTO role_permissions (role_id, permission)
SELECT r.id, g.permission
FROM roles r
JOIN (VALUES
    ('manager', 'warehouses:view'), ('manager', 'warehouses:create'), ('manager', 'warehouses:update'),
    ('manager', 'shelves:view'),    ('manager', 'shelves:create'),    ('manager', 'shelves:update'),
    ('manager', 'customers:view'),  ('manager', 'customers:create'),  ('manager', 'customers:update'),
    ('staff',   'warehouses:view'), ('staff',   'shelves:view'),
    ('staff',   'customers:view'),  ('staff',   'customers:create'),  ('staff',   'customers:update'),
    ('viewer',  'warehouses:view'), ('viewer',  'shelves:view'),      ('viewer',  'customers:view')
) AS g (role_name, permission) ON g.role_name = r.name
WHERE r.is_system
ON CONFLICT DO NOTHING;

INSERT INTO role_pages (role_id, page_path)
SELECT r.id, g.page_path
FROM roles r
JOIN (VALUES
    ('manager', '/warehouses'), ('manager', '/shelves'), ('manager', '/rows'), ('manager', '/containers'),
    ('manager', '/customers'),  ('manager', '/reports'),
    ('staff',   '/warehouses'), ('staff',   '/shelves'), ('staff',   '/rows'), ('staff',   '/containers'),
    ('staff',   '/customers'),
    ('viewer',  '/warehouses'), ('viewer',  '/shelves'), ('viewer',  '/rows'), ('viewer',  '/containers'),
    ('viewer',  '/customers')
) AS g (role_name, page_path) ON g.role_name = r.name
WHERE r.is_system
ON CONFLICT DO NOTHING;
