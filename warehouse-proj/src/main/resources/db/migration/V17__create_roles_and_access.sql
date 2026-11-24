-- Roles, the permissions each role holds, and the pages each role may open.
-- The four standard roles are created at startup by AccessBootstrap (so they always exist, seed data or not).
CREATE TABLE roles (
    id           UUID         PRIMARY KEY,
    name         VARCHAR(50)  NOT NULL,
    description  VARCHAR(255),
    is_system    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ,
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_roles_name UNIQUE (name)
);

-- e.g. 'inventory:update'
CREATE TABLE role_permissions (
    role_id     UUID         NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    permission  VARCHAR(100) NOT NULL,
    PRIMARY KEY (role_id, permission)
);

-- Page assignment, e.g. '/orders'
CREATE TABLE role_pages (
    role_id    UUID         NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    page_path  VARCHAR(100) NOT NULL,
    PRIMARY KEY (role_id, page_path)
);

ALTER TABLE app_users ADD COLUMN role_id UUID REFERENCES roles (id);
CREATE INDEX idx_app_users_role ON app_users (role_id);
