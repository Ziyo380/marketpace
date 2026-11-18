CREATE TABLE roles (
    id UUID PRIMARY KEY,
    name VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_roles_name UNIQUE (name),
    CONSTRAINT chk_roles_name CHECK (name IN ('CUSTOMER', 'SELLER', 'ADMIN'))
);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'BLOCKED'))
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE RESTRICT
);

CREATE INDEX idx_user_roles_role_id ON user_roles (role_id);

INSERT INTO roles (id, name, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'CUSTOMER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000000002', 'SELLER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000000003', 'ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
