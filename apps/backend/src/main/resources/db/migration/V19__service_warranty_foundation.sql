CREATE TABLE service_warranty_policy (
    id uuid PRIMARY KEY,
    company_id uuid NOT NULL REFERENCES company(id),
    product_id uuid,
    duration_days integer NOT NULL,
    serial_required boolean NOT NULL,
    sales_evidence_required boolean NOT NULL,
    effective_from date NOT NULL,
    effective_to date,
    active boolean NOT NULL,
    CONSTRAINT ck_service_policy_duration CHECK (duration_days > 0),
    CONSTRAINT ck_service_policy_dates CHECK (
        effective_to IS NULL OR effective_to >= effective_from
    ),
    CONSTRAINT uq_service_policy_scope UNIQUE (company_id, product_id, effective_from)
);

CREATE TABLE service_ticket (
    id uuid PRIMARY KEY,
    idempotency_key varchar(160) NOT NULL UNIQUE,
    ticket_number varchar(120) NOT NULL UNIQUE,
    company_id uuid NOT NULL REFERENCES company(id),
    branch_id uuid,
    customer_id uuid NOT NULL,
    product_id uuid NOT NULL,
    sku_id uuid NOT NULL,
    serial_code varchar(120) NOT NULL DEFAULT '',
    sales_order_id uuid,
    purchase_date date,
    issue_summary varchar(1000) NOT NULL,
    status varchar(32) NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    actor varchar(160) NOT NULL,
    CONSTRAINT fk_service_ticket_branch_scope
        FOREIGN KEY (branch_id, company_id) REFERENCES branch(id, company_id),
    CONSTRAINT ck_service_ticket_status CHECK (status IN (
        'OPEN', 'VALIDATING', 'WARRANTY_VALID', 'WARRANTY_REJECTED',
        'AWAITING_APPROVAL', 'REPAIRING', 'REPLACING', 'CLOSED', 'CANCELLED'
    )),
    CONSTRAINT ck_service_ticket_version CHECK (version >= 0)
);

CREATE TABLE service_warranty_decision (
    id uuid PRIMARY KEY,
    ticket_id uuid NOT NULL UNIQUE REFERENCES service_ticket(id),
    policy_id uuid NOT NULL REFERENCES service_warranty_policy(id),
    eligible boolean NOT NULL,
    reason varchar(1000) NOT NULL,
    coverage_ends_on date,
    decided_at timestamptz NOT NULL,
    actor varchar(160) NOT NULL
);

CREATE TABLE service_diagnosis (
    id uuid PRIMARY KEY,
    ticket_id uuid NOT NULL UNIQUE REFERENCES service_ticket(id),
    findings varchar(4000) NOT NULL,
    recommendation varchar(2000) NOT NULL,
    diagnosed_at timestamptz NOT NULL
);

CREATE TABLE service_resolution (
    id uuid PRIMARY KEY,
    ticket_id uuid NOT NULL UNIQUE REFERENCES service_ticket(id),
    resolution_type varchar(24) NOT NULL,
    outcome varchar(4000) NOT NULL,
    resolved_at timestamptz NOT NULL,
    CONSTRAINT ck_service_resolution_type CHECK (
        resolution_type IN ('REPAIR', 'REPLACEMENT', 'CANCELLED')
    )
);

CREATE INDEX ix_service_policy_resolution
    ON service_warranty_policy(company_id, product_id, active, effective_from);
CREATE INDEX ix_service_ticket_company_status
    ON service_ticket(company_id, status, updated_at);
CREATE INDEX ix_service_ticket_customer ON service_ticket(customer_id, created_at);
CREATE INDEX ix_service_ticket_serial ON service_ticket(sku_id, serial_code);

INSERT INTO iam_permission (id, capability, description)
VALUES
    ('3b000000-0000-4000-8000-000000000001', 'service.ticket.manage',
     'Create company-scoped service tickets'),
    ('3b000000-0000-4000-8000-000000000002', 'service.warranty.validate',
     'Validate company-scoped warranty claims'),
    ('3b000000-0000-4000-8000-000000000003', 'service.ticket.diagnose',
     'Record service diagnosis'),
    ('3b000000-0000-4000-8000-000000000004', 'service.ticket.approve-resolution',
     'Approve repair or replacement resolution'),
    ('3b000000-0000-4000-8000-000000000005', 'service.ticket.close',
     'Close resolved service tickets'),
    ('3b000000-0000-4000-8000-000000000006', 'service.warranty-policy.manage',
     'Manage company-scoped warranty policies')
ON CONFLICT (capability) DO NOTHING;

INSERT INTO platform_domain_event_catalog (event_type, owner_context, description)
VALUES
    ('ServiceTicketCreated', 'servicewarranty', 'A service ticket was created'),
    ('WarrantyValidated', 'servicewarranty', 'A warranty decision was recorded'),
    ('ServiceTicketClosed', 'servicewarranty', 'A service ticket was closed')
ON CONFLICT (event_type) DO NOTHING;
