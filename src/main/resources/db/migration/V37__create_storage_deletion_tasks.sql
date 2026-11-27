-- Persist deletion intent in the same transaction as ProductImage removal.
CREATE TABLE storage_deletion_tasks (
    id UUID PRIMARY KEY,
    object_key VARCHAR(512) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_storage_deletion_tasks_object_key UNIQUE (object_key),
    CONSTRAINT chk_storage_deletion_tasks_attempts CHECK (attempts >= 0)
);

CREATE INDEX idx_storage_deletion_tasks_due ON storage_deletion_tasks (next_attempt_at, id);
