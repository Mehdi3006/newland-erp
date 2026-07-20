CREATE TABLE iam_user (
    id uuid PRIMARY KEY,
    username varchar(120) NOT NULL,
    email varchar(160) NOT NULL,
    display_name varchar(160) NOT NULL,
    status varchar(16) NOT NULL,
    failed_login_attempts integer NOT NULL DEFAULT 0,
    locked_until timestamptz,
    password_expires_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT uq_iam_user_username UNIQUE (username),
    CONSTRAINT uq_iam_user_email UNIQUE (email),
    CONSTRAINT ck_iam_user_status CHECK (status IN ('INVITED', 'ACTIVE', 'LOCKED', 'DISABLED')),
    CONSTRAINT ck_iam_user_failed_attempts CHECK (failed_login_attempts >= 0)
);

CREATE TABLE iam_role (
    id uuid PRIMARY KEY,
    code varchar(64) NOT NULL,
    name varchar(160) NOT NULL,
    description varchar(500),
    system_role boolean NOT NULL DEFAULT false,
    CONSTRAINT uq_iam_role_code UNIQUE (code)
);

CREATE TABLE iam_permission (
    id uuid PRIMARY KEY,
    capability varchar(160) NOT NULL,
    description varchar(500),
    CONSTRAINT uq_iam_permission_capability UNIQUE (capability),
    CONSTRAINT ck_iam_permission_capability CHECK (capability ~ '^[a-z][a-z0-9-]*(\.[a-z][a-z0-9-]*)+$')
);

CREATE TABLE iam_user_role_assignment (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES iam_user (id),
    role_id uuid NOT NULL REFERENCES iam_role (id),
    scope_type varchar(32) NOT NULL,
    scope_id uuid NOT NULL,
    assigned_at timestamptz NOT NULL,
    CONSTRAINT uq_iam_user_role_scope UNIQUE (user_id, role_id, scope_type, scope_id),
    CONSTRAINT ck_iam_scope_type CHECK (
        scope_type IN ('ENTERPRISE', 'LEGAL_ENTITY', 'COMPANY', 'BRANCH', 'WAREHOUSE')
    )
);

CREATE TABLE iam_role_permission_assignment (
    id uuid PRIMARY KEY,
    role_id uuid NOT NULL REFERENCES iam_role (id),
    permission_id uuid NOT NULL REFERENCES iam_permission (id),
    assigned_at timestamptz NOT NULL,
    CONSTRAINT uq_iam_role_permission UNIQUE (role_id, permission_id)
);

CREATE TABLE iam_password_credential (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES iam_user (id),
    password_hash varchar(500) NOT NULL,
    changed_at timestamptz NOT NULL,
    expires_at timestamptz,
    current_credential boolean NOT NULL,
    CONSTRAINT ck_iam_password_argon2 CHECK (password_hash LIKE '$argon2%')
);

CREATE TABLE iam_session (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES iam_user (id),
    device_label varchar(160),
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz
);

CREATE TABLE iam_refresh_token (
    id uuid PRIMARY KEY,
    session_id uuid NOT NULL REFERENCES iam_session (id),
    user_id uuid NOT NULL REFERENCES iam_user (id),
    token_hash char(64) NOT NULL,
    issued_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    rotated_at timestamptz,
    revoked_at timestamptz,
    CONSTRAINT uq_iam_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_iam_refresh_token_hash CHECK (token_hash ~ '^[0-9a-f]{64}$')
);

CREATE INDEX ix_iam_user_role_user ON iam_user_role_assignment (user_id);
CREATE INDEX ix_iam_role_permission_role ON iam_role_permission_assignment (role_id);
CREATE INDEX ix_iam_session_user ON iam_session (user_id);
CREATE INDEX ix_iam_refresh_token_session ON iam_refresh_token (session_id);
