-- One row per recipient user: "mark as read" and "delete" are per user.
CREATE TABLE notifications (
    id            UUID          PRIMARY KEY,
    recipient_id  UUID          NOT NULL REFERENCES app_users (id),
    type          VARCHAR(30)   NOT NULL,
    title         VARCHAR(200)  NOT NULL,
    message       VARCHAR(1000) NOT NULL,
    entity_name   VARCHAR(100),
    entity_id     UUID,
    is_read       BOOLEAN       NOT NULL DEFAULT FALSE,
    read_at       TIMESTAMPTZ,
    created_at    TIMESTAMPTZ   NOT NULL,
    updated_at    TIMESTAMPTZ,
    is_active     BOOLEAN       NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_notifications_recipient ON notifications (recipient_id, created_at DESC);
