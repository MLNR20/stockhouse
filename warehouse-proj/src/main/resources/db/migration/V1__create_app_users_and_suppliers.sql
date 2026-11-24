CREATE TABLE app_users (
    id                        UUID         PRIMARY KEY,
    first_name                VARCHAR(100) NOT NULL,
    last_name                 VARCHAR(100) NOT NULL,
    email                     VARCHAR(256) NOT NULL,
    password_hash             VARCHAR(255) NOT NULL,
    refresh_token             VARCHAR(512),
    refresh_token_expires_at  TIMESTAMPTZ,
    created_at                TIMESTAMPTZ  NOT NULL,
    updated_at                TIMESTAMPTZ,
    is_active                 BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_app_users_email UNIQUE (email)
);

CREATE TABLE suppliers (
    id             UUID         PRIMARY KEY,
    supplier_name  VARCHAR(200) NOT NULL,
    phone_number   VARCHAR(30)  NOT NULL,
    email          VARCHAR(256) NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE
);
