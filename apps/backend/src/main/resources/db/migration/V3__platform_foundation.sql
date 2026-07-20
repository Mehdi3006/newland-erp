CREATE TABLE platform_outbox (
    id uuid PRIMARY KEY,
    event_id uuid NOT NULL,
    source_context varchar(80) NOT NULL,
    event_type varchar(120) NOT NULL,
    aggregate_id uuid NOT NULL,
    occurred_at timestamptz NOT NULL,
    payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    status varchar(16) NOT NULL,
    attempts integer NOT NULL DEFAULT 0,
    next_attempt_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL,
    published_at timestamptz,
    last_error varchar(1000),
    CONSTRAINT uq_platform_outbox_event UNIQUE (event_id),
    CONSTRAINT ck_platform_outbox_status CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    CONSTRAINT ck_platform_outbox_attempts CHECK (attempts >= 0)
);

CREATE TABLE platform_audit_log (
    id uuid PRIMARY KEY,
    actor varchar(160) NOT NULL,
    action varchar(120) NOT NULL,
    target_type varchar(120) NOT NULL,
    target_id uuid NOT NULL,
    occurred_at timestamptz NOT NULL,
    attributes jsonb NOT NULL DEFAULT '{}'::jsonb
);

CREATE TABLE platform_background_job (
    id uuid PRIMARY KEY,
    job_type varchar(120) NOT NULL,
    status varchar(16) NOT NULL,
    scheduled_at timestamptz NOT NULL,
    started_at timestamptz,
    completed_at timestamptz,
    parameters jsonb NOT NULL DEFAULT '{}'::jsonb,
    last_error varchar(1000),
    CONSTRAINT ck_platform_job_status CHECK (status IN ('SCHEDULED', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED'))
);

CREATE TABLE platform_stored_file (
    id uuid PRIMARY KEY,
    storage_key varchar(240) NOT NULL,
    file_name varchar(240) NOT NULL,
    content_type varchar(120) NOT NULL,
    size_bytes bigint NOT NULL,
    checksum_sha256 char(64) NOT NULL,
    created_at timestamptz NOT NULL,
    CONSTRAINT uq_platform_storage_key UNIQUE (storage_key),
    CONSTRAINT ck_platform_file_size CHECK (size_bytes >= 0),
    CONSTRAINT ck_platform_file_checksum CHECK (checksum_sha256 ~ '^[0-9a-f]{64}$')
);

CREATE TABLE platform_attachment (
    id uuid PRIMARY KEY,
    owner_context varchar(80) NOT NULL,
    owner_type varchar(120) NOT NULL,
    owner_id uuid NOT NULL,
    file_id uuid NOT NULL REFERENCES platform_stored_file (id),
    attached_at timestamptz NOT NULL
);

CREATE TABLE platform_configuration (
    config_key varchar(160) PRIMARY KEY,
    config_value text NOT NULL,
    encrypted boolean NOT NULL DEFAULT false,
    updated_at timestamptz NOT NULL,
    updated_by varchar(160) NOT NULL
);

CREATE TABLE platform_feature_flag (
    flag_key varchar(160) PRIMARY KEY,
    enabled boolean NOT NULL,
    description varchar(500),
    updated_at timestamptz NOT NULL,
    updated_by varchar(160) NOT NULL
);

CREATE TABLE platform_localization_message (
    locale varchar(16) NOT NULL,
    message_key varchar(240) NOT NULL,
    message text NOT NULL,
    PRIMARY KEY (locale, message_key)
);

CREATE TABLE platform_error_catalog (
    code varchar(120) PRIMARY KEY,
    http_status varchar(3) NOT NULL,
    title varchar(240) NOT NULL,
    owner_context varchar(80) NOT NULL
);

CREATE TABLE platform_domain_event_catalog (
    event_type varchar(120) PRIMARY KEY,
    owner_context varchar(80) NOT NULL,
    description varchar(500) NOT NULL
);

CREATE INDEX ix_platform_outbox_pending ON platform_outbox (status, next_attempt_at);
CREATE INDEX ix_platform_audit_target ON platform_audit_log (target_type, target_id);
CREATE INDEX ix_platform_job_status ON platform_background_job (status, scheduled_at);
CREATE INDEX ix_platform_attachment_owner ON platform_attachment (owner_context, owner_type, owner_id);
