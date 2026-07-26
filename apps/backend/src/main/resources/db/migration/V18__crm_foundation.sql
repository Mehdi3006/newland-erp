CREATE TABLE crm_lead (
    id uuid PRIMARY KEY,
    idempotency_key varchar(160) NOT NULL UNIQUE,
    company_id uuid NOT NULL REFERENCES company(id),
    branch_id uuid,
    owner_id uuid NOT NULL REFERENCES iam_user(id),
    lead_number varchar(120) NOT NULL UNIQUE,
    organization_name varchar(240) NOT NULL,
    contact_name varchar(160) NOT NULL,
    email varchar(240) NOT NULL DEFAULT '',
    phone varchar(80) NOT NULL DEFAULT '',
    source varchar(80) NOT NULL,
    status varchar(24) NOT NULL,
    disposition_reason varchar(500) NOT NULL DEFAULT '',
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    actor varchar(160) NOT NULL,
    CONSTRAINT fk_crm_lead_branch_scope
        FOREIGN KEY (branch_id, company_id) REFERENCES branch(id, company_id),
    CONSTRAINT ck_crm_lead_contact CHECK (email <> '' OR phone <> ''),
    CONSTRAINT ck_crm_lead_status CHECK (
        status IN ('NEW', 'QUALIFIED', 'DISQUALIFIED', 'CONVERTED')
    ),
    CONSTRAINT ck_crm_lead_disposition CHECK (
        (status IN ('DISQUALIFIED', 'CONVERTED') AND disposition_reason <> '')
        OR (status IN ('NEW', 'QUALIFIED'))
    ),
    CONSTRAINT ck_crm_lead_version CHECK (version >= 0)
);

CREATE TABLE crm_opportunity (
    id uuid PRIMARY KEY,
    idempotency_key varchar(160) NOT NULL UNIQUE,
    company_id uuid NOT NULL REFERENCES company(id),
    branch_id uuid,
    owner_id uuid NOT NULL REFERENCES iam_user(id),
    lead_id uuid REFERENCES crm_lead(id),
    customer_id uuid,
    opportunity_number varchar(120) NOT NULL UNIQUE,
    name varchar(240) NOT NULL,
    stage varchar(24) NOT NULL,
    estimated_value numeric(19,6) NOT NULL,
    currency_code varchar(12) NOT NULL,
    probability_percent integer NOT NULL,
    expected_close_date date NOT NULL,
    closure_reason varchar(500) NOT NULL DEFAULT '',
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    actor varchar(160) NOT NULL,
    CONSTRAINT fk_crm_opportunity_branch_scope
        FOREIGN KEY (branch_id, company_id) REFERENCES branch(id, company_id),
    CONSTRAINT ck_crm_opportunity_reference CHECK (lead_id IS NOT NULL OR customer_id IS NOT NULL),
    CONSTRAINT ck_crm_opportunity_stage CHECK (
        stage IN ('QUALIFICATION', 'DISCOVERY', 'PROPOSAL', 'NEGOTIATION', 'WON', 'LOST')
    ),
    CONSTRAINT ck_crm_opportunity_value CHECK (estimated_value >= 0),
    CONSTRAINT ck_crm_opportunity_probability CHECK (probability_percent BETWEEN 0 AND 100),
    CONSTRAINT ck_crm_opportunity_closure CHECK (
        (stage IN ('WON', 'LOST') AND closure_reason <> '')
        OR (stage NOT IN ('WON', 'LOST'))
    ),
    CONSTRAINT ck_crm_opportunity_version CHECK (version >= 0)
);

CREATE TABLE crm_activity (
    id uuid PRIMARY KEY,
    idempotency_key varchar(160) NOT NULL UNIQUE,
    company_id uuid NOT NULL REFERENCES company(id),
    customer_id uuid,
    lead_id uuid REFERENCES crm_lead(id),
    opportunity_id uuid REFERENCES crm_opportunity(id),
    activity_type varchar(24) NOT NULL,
    subject varchar(240) NOT NULL,
    details varchar(4000) NOT NULL DEFAULT '',
    occurred_at timestamptz NOT NULL,
    follow_up_at timestamptz,
    actor varchar(160) NOT NULL,
    CONSTRAINT ck_crm_activity_reference CHECK (
        customer_id IS NOT NULL OR lead_id IS NOT NULL OR opportunity_id IS NOT NULL
    ),
    CONSTRAINT ck_crm_activity_type CHECK (
        activity_type IN ('CALL', 'EMAIL', 'MEETING', 'NOTE', 'TASK')
    ),
    CONSTRAINT ck_crm_activity_follow_up CHECK (
        follow_up_at IS NULL OR follow_up_at >= occurred_at
    )
);

CREATE INDEX ix_crm_lead_company_status ON crm_lead(company_id, status, updated_at);
CREATE INDEX ix_crm_opportunity_company_stage
    ON crm_opportunity(company_id, stage, expected_close_date);
CREATE INDEX ix_crm_activity_customer_timeline
    ON crm_activity(company_id, customer_id, occurred_at DESC);
CREATE INDEX ix_crm_activity_lead ON crm_activity(lead_id, occurred_at DESC);
CREATE INDEX ix_crm_activity_opportunity ON crm_activity(opportunity_id, occurred_at DESC);

INSERT INTO iam_permission (id, capability, description)
VALUES
    ('3a000000-0000-4000-8000-000000000001', 'crm.lead.manage',
     'Create company-scoped CRM leads'),
    ('3a000000-0000-4000-8000-000000000002', 'crm.lead.qualify',
     'Qualify or disqualify company-scoped CRM leads'),
    ('3a000000-0000-4000-8000-000000000003', 'crm.opportunity.manage',
     'Create and progress company-scoped CRM opportunities'),
    ('3a000000-0000-4000-8000-000000000004', 'crm.activity.create',
     'Record company-scoped CRM activities'),
    ('3a000000-0000-4000-8000-000000000005', 'crm.timeline.read',
     'Read company-scoped customer activity timelines')
ON CONFLICT (capability) DO NOTHING;

INSERT INTO platform_domain_event_catalog (event_type, owner_context, description)
VALUES
    ('LeadQualified', 'crm', 'A CRM lead was qualified'),
    ('OpportunityWon', 'crm', 'A CRM opportunity was won'),
    ('OpportunityLost', 'crm', 'A CRM opportunity was lost')
ON CONFLICT (event_type) DO NOTHING;
