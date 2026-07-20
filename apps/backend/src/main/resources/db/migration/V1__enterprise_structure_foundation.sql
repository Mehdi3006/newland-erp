CREATE TABLE enterprise (
    id uuid PRIMARY KEY,
    code varchar(32) NOT NULL,
    name varchar(160) NOT NULL,
    localized_name jsonb NOT NULL DEFAULT '{}'::jsonb,
    status varchar(16) NOT NULL,
    created_at timestamptz NOT NULL,
    created_by varchar(120) NOT NULL,
    updated_at timestamptz NOT NULL,
    updated_by varchar(120) NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT uq_enterprise_code UNIQUE (code),
    CONSTRAINT ck_enterprise_status CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_enterprise_version CHECK (version >= 0)
);

CREATE TABLE legal_entity (
    id uuid PRIMARY KEY,
    enterprise_id uuid NOT NULL REFERENCES enterprise (id),
    code varchar(32) NOT NULL,
    name varchar(160) NOT NULL,
    localized_name jsonb NOT NULL DEFAULT '{}'::jsonb,
    country_code char(2) NOT NULL,
    base_currency char(3) NOT NULL,
    status varchar(16) NOT NULL,
    created_at timestamptz NOT NULL,
    created_by varchar(120) NOT NULL,
    updated_at timestamptz NOT NULL,
    updated_by varchar(120) NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT uq_legal_entity_enterprise_code UNIQUE (enterprise_id, code),
    CONSTRAINT ck_legal_entity_status CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_legal_entity_country CHECK (country_code ~ '^[A-Z]{2}$'),
    CONSTRAINT ck_legal_entity_currency CHECK (base_currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_legal_entity_version CHECK (version >= 0)
);

CREATE TABLE company (
    id uuid PRIMARY KEY,
    enterprise_id uuid NOT NULL REFERENCES enterprise (id),
    legal_entity_id uuid NOT NULL REFERENCES legal_entity (id),
    code varchar(32) NOT NULL,
    name varchar(160) NOT NULL,
    localized_name jsonb NOT NULL DEFAULT '{}'::jsonb,
    country_code char(2) NOT NULL,
    base_currency char(3) NOT NULL,
    time_zone_id varchar(64) NOT NULL,
    address_line1 varchar(160),
    address_line2 varchar(160),
    city varchar(80),
    region varchar(80),
    postal_code varchar(32),
    status varchar(16) NOT NULL,
    created_at timestamptz NOT NULL,
    created_by varchar(120) NOT NULL,
    updated_at timestamptz NOT NULL,
    updated_by varchar(120) NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT uq_company_enterprise_code UNIQUE (enterprise_id, code),
    CONSTRAINT uq_company_id_enterprise UNIQUE (id, enterprise_id),
    CONSTRAINT ck_company_status CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_company_country CHECK (country_code ~ '^[A-Z]{2}$'),
    CONSTRAINT ck_company_currency CHECK (base_currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_company_version CHECK (version >= 0)
);

CREATE TABLE branch (
    id uuid PRIMARY KEY,
    enterprise_id uuid NOT NULL REFERENCES enterprise (id),
    company_id uuid NOT NULL REFERENCES company (id),
    code varchar(32) NOT NULL,
    name varchar(160) NOT NULL,
    localized_name jsonb NOT NULL DEFAULT '{}'::jsonb,
    address_line1 varchar(160),
    address_line2 varchar(160),
    city varchar(80),
    region varchar(80),
    postal_code varchar(32),
    status varchar(16) NOT NULL,
    created_at timestamptz NOT NULL,
    created_by varchar(120) NOT NULL,
    updated_at timestamptz NOT NULL,
    updated_by varchar(120) NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT uq_branch_company_code UNIQUE (company_id, code),
    CONSTRAINT uq_branch_id_company UNIQUE (id, company_id),
    CONSTRAINT ck_branch_status CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_branch_version CHECK (version >= 0),
    CONSTRAINT fk_branch_company_scope FOREIGN KEY (company_id, enterprise_id) REFERENCES company (id, enterprise_id)
);

CREATE TABLE warehouse (
    id uuid PRIMARY KEY,
    enterprise_id uuid NOT NULL REFERENCES enterprise (id),
    company_id uuid NOT NULL REFERENCES company (id),
    branch_id uuid,
    code varchar(32) NOT NULL,
    name varchar(160) NOT NULL,
    localized_name jsonb NOT NULL DEFAULT '{}'::jsonb,
    warehouse_type varchar(16) NOT NULL,
    project_reference varchar(80),
    address_line1 varchar(160),
    address_line2 varchar(160),
    city varchar(80),
    region varchar(80),
    postal_code varchar(32),
    status varchar(16) NOT NULL,
    created_at timestamptz NOT NULL,
    created_by varchar(120) NOT NULL,
    updated_at timestamptz NOT NULL,
    updated_by varchar(120) NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT uq_warehouse_company_code UNIQUE (company_id, code),
    CONSTRAINT uq_warehouse_id_company UNIQUE (id, company_id),
    CONSTRAINT ck_warehouse_status CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_warehouse_type CHECK (warehouse_type IN ('CENTRAL', 'BRANCH', 'PROJECT')),
    CONSTRAINT ck_warehouse_branch_type CHECK (
        (warehouse_type = 'BRANCH' AND branch_id IS NOT NULL)
            OR (warehouse_type <> 'BRANCH')
    ),
    CONSTRAINT ck_warehouse_project_type CHECK (
        (warehouse_type = 'PROJECT' AND project_reference IS NOT NULL)
            OR (warehouse_type <> 'PROJECT')
    ),
    CONSTRAINT ck_warehouse_version CHECK (version >= 0),
    CONSTRAINT fk_warehouse_company_scope FOREIGN KEY (company_id, enterprise_id) REFERENCES company (id, enterprise_id),
    CONSTRAINT fk_warehouse_branch_scope FOREIGN KEY (branch_id, company_id) REFERENCES branch (id, company_id)
);

CREATE TABLE warehouse_zone (
    id uuid PRIMARY KEY,
    enterprise_id uuid NOT NULL REFERENCES enterprise (id),
    company_id uuid NOT NULL REFERENCES company (id),
    warehouse_id uuid NOT NULL REFERENCES warehouse (id),
    code varchar(32) NOT NULL,
    name varchar(160) NOT NULL,
    localized_name jsonb NOT NULL DEFAULT '{}'::jsonb,
    status varchar(16) NOT NULL,
    created_at timestamptz NOT NULL,
    created_by varchar(120) NOT NULL,
    updated_at timestamptz NOT NULL,
    updated_by varchar(120) NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT uq_zone_warehouse_code UNIQUE (warehouse_id, code),
    CONSTRAINT uq_zone_id_warehouse UNIQUE (id, warehouse_id),
    CONSTRAINT ck_zone_status CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_zone_version CHECK (version >= 0),
    CONSTRAINT fk_zone_warehouse_scope FOREIGN KEY (warehouse_id, company_id) REFERENCES warehouse (id, company_id)
);

CREATE TABLE warehouse_location (
    id uuid PRIMARY KEY,
    enterprise_id uuid NOT NULL REFERENCES enterprise (id),
    company_id uuid NOT NULL REFERENCES company (id),
    warehouse_id uuid NOT NULL REFERENCES warehouse (id),
    zone_id uuid NOT NULL REFERENCES warehouse_zone (id),
    code varchar(32) NOT NULL,
    name varchar(160) NOT NULL,
    localized_name jsonb NOT NULL DEFAULT '{}'::jsonb,
    status varchar(16) NOT NULL,
    created_at timestamptz NOT NULL,
    created_by varchar(120) NOT NULL,
    updated_at timestamptz NOT NULL,
    updated_by varchar(120) NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT uq_location_zone_code UNIQUE (zone_id, code),
    CONSTRAINT ck_location_status CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_location_version CHECK (version >= 0),
    CONSTRAINT fk_location_warehouse_scope FOREIGN KEY (warehouse_id, company_id) REFERENCES warehouse (id, company_id),
    CONSTRAINT fk_location_zone_scope FOREIGN KEY (zone_id, warehouse_id) REFERENCES warehouse_zone (id, warehouse_id)
);

CREATE INDEX ix_legal_entity_enterprise ON legal_entity (enterprise_id);
CREATE INDEX ix_company_legal_entity ON company (legal_entity_id);
CREATE INDEX ix_branch_company ON branch (company_id);
CREATE INDEX ix_warehouse_company ON warehouse (company_id);
CREATE INDEX ix_warehouse_branch ON warehouse (branch_id);
CREATE INDEX ix_zone_warehouse ON warehouse_zone (warehouse_id);
CREATE INDEX ix_location_zone ON warehouse_location (zone_id);
