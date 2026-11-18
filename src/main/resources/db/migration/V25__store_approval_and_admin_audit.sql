ALTER TABLE stores ADD COLUMN approved BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE stores ADD COLUMN rejection_reason VARCHAR(500);
ALTER TABLE products ADD COLUMN moderation_blocked BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE admin_audit_logs (
    id UUID PRIMARY KEY,
    actor_id UUID NOT NULL REFERENCES users(id),
    action VARCHAR(40) NOT NULL,
    target_type VARCHAR(40) NOT NULL,
    target_id UUID NOT NULL,
    details VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_admin_audit_logs_created ON admin_audit_logs(created_at DESC);
