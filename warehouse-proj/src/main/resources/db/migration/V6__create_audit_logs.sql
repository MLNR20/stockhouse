CREATE TABLE audit_logs (
    id            UUID          PRIMARY KEY,
    entity_name   VARCHAR(100)  NOT NULL,
    entity_id     UUID          NOT NULL,
    action        VARCHAR(20)   NOT NULL,
    performed_by  VARCHAR(256)  NOT NULL,
    created_at    TIMESTAMPTZ   NOT NULL,
    updated_at    TIMESTAMPTZ,
    is_active     BOOLEAN       NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_name, entity_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at DESC);
