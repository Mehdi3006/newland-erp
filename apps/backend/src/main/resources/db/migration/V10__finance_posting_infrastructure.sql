CREATE TABLE finance_accounting_event (
    event_id uuid PRIMARY KEY, idempotency_key varchar(160) NOT NULL UNIQUE, event_type varchar(120) NOT NULL,
    source_module varchar(120) NOT NULL, source_document_type varchar(120) NOT NULL, source_document_id uuid NOT NULL,
    source_document_number varchar(120), company_id uuid NOT NULL, branch_id uuid NOT NULL, event_date date NOT NULL,
    accounting_date date NOT NULL, currency_code varchar(12) NOT NULL, exchange_rate numeric(19,8) NOT NULL,
    amount numeric(19,6) NOT NULL, tax_amount numeric(19,6), net_amount numeric(19,6), description varchar(500),
    dimensions jsonb NOT NULL, attributes jsonb NOT NULL, occurred_at timestamptz NOT NULL, submitted_by varchar(160) NOT NULL,
    version integer NOT NULL DEFAULT 1, accepted_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_finance_event_rate CHECK (exchange_rate > 0), CONSTRAINT ck_finance_event_amount CHECK (amount >= 0)
);
CREATE TABLE finance_posting_rule (
    posting_rule_id uuid PRIMARY KEY, code varchar(120) NOT NULL, name varchar(240) NOT NULL, event_type varchar(120) NOT NULL,
    company_id uuid, effective_from date NOT NULL, effective_to date, priority integer NOT NULL, status varchar(20) NOT NULL,
    version integer NOT NULL, created_at timestamptz NOT NULL, created_by varchar(160) NOT NULL, updated_at timestamptz,
    updated_by varchar(160), CONSTRAINT uq_finance_posting_rule_scope UNIQUE (code, company_id, version),
    CONSTRAINT ck_finance_posting_rule_dates CHECK (effective_to IS NULL OR effective_to >= effective_from)
);
CREATE TABLE finance_posting_rule_line (
    posting_rule_line_id uuid PRIMARY KEY, posting_rule_id uuid NOT NULL REFERENCES finance_posting_rule(posting_rule_id),
    line_number integer NOT NULL, direction varchar(8) NOT NULL, account_resolution_type varchar(40) NOT NULL,
    fixed_account_id uuid, account_attribute_key varchar(120), amount_expression varchar(40) NOT NULL,
    constant_amount numeric(19,6), description_template varchar(500), dimension_mappings jsonb NOT NULL,
    CONSTRAINT uq_finance_posting_rule_line UNIQUE (posting_rule_id, line_number)
);
CREATE TABLE finance_posting_request (
    posting_request_id uuid PRIMARY KEY, accounting_event_id uuid NOT NULL UNIQUE REFERENCES finance_accounting_event(event_id),
    status varchar(24) NOT NULL, resolved_posting_rule_id uuid REFERENCES finance_posting_rule(posting_rule_id),
    resolved_posting_rule_version integer, journal_entry_id uuid, failure_code varchar(120), failure_message varchar(1000),
    attempts integer NOT NULL DEFAULT 0, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
    version integer NOT NULL DEFAULT 0
);
CREATE INDEX ix_finance_posting_rule_lookup ON finance_posting_rule(event_type, company_id, status, effective_from, effective_to);
CREATE INDEX ix_finance_posting_event_source ON finance_accounting_event(source_module, source_document_id);
