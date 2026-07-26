CREATE TABLE procurement_accounting_publication (
    event_id uuid PRIMARY KEY,
    idempotency_key varchar(160) NOT NULL UNIQUE,
    event_type varchar(80) NOT NULL,
    reference_document_type varchar(120) NOT NULL,
    reference_document_id uuid NOT NULL,
    reference_document_number varchar(120) NOT NULL,
    supplier_id uuid NOT NULL REFERENCES procurement_supplier(id),
    company_id uuid NOT NULL REFERENCES company(id),
    branch_id uuid NOT NULL,
    event_date date NOT NULL,
    accounting_date date NOT NULL,
    currency_code varchar(12) NOT NULL,
    exchange_rate numeric(19,8) NOT NULL,
    amount numeric(19,6) NOT NULL,
    tax_amount numeric(19,6) NOT NULL,
    net_amount numeric(19,6) NOT NULL,
    cost_center_id uuid,
    profit_center_id uuid,
    financial_dimensions jsonb NOT NULL,
    description varchar(500) NOT NULL,
    occurred_at timestamptz NOT NULL,
    actor varchar(160) NOT NULL,
    status varchar(24) NOT NULL,
    posting_request_id uuid,
    journal_entry_id uuid,
    journal_number varchar(120),
    failure_code varchar(120),
    failure_message varchar(1000),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_procurement_accounting_branch
        FOREIGN KEY (branch_id, company_id) REFERENCES branch(id, company_id),
    CONSTRAINT fk_procurement_accounting_request
        FOREIGN KEY (posting_request_id) REFERENCES finance_posting_request(posting_request_id),
    CONSTRAINT fk_procurement_accounting_journal
        FOREIGN KEY (journal_entry_id) REFERENCES finance_journal_entry(id),
    CONSTRAINT ck_procurement_accounting_status
        CHECK (status IN ('PENDING', 'POSTED', 'REJECTED')),
    CONSTRAINT ck_procurement_accounting_rate CHECK (exchange_rate > 0),
    CONSTRAINT ck_procurement_accounting_amounts
        CHECK (amount >= 0 AND tax_amount >= 0 AND net_amount >= 0)
);

CREATE INDEX ix_procurement_accounting_status
    ON procurement_accounting_publication(status, created_at);
CREATE INDEX ix_procurement_accounting_reference
    ON procurement_accounting_publication(reference_document_type, reference_document_id);
