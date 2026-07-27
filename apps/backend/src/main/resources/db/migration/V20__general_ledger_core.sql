ALTER TABLE finance_accounting_period
    ADD COLUMN period_state varchar(16);

UPDATE finance_accounting_period
SET period_state = CASE WHEN closed THEN 'CLOSED' ELSE 'OPEN' END;

ALTER TABLE finance_accounting_period
    ALTER COLUMN period_state SET NOT NULL,
    ADD CONSTRAINT ck_finance_period_state
        CHECK (period_state IN ('OPEN', 'CLOSING', 'CLOSED')),
    ADD CONSTRAINT ck_finance_period_closed_consistency
        CHECK (closed = (period_state = 'CLOSED'));

ALTER TABLE finance_journal_entry
    ADD CONSTRAINT uq_finance_journal_id_company UNIQUE (id, company_id);

ALTER TABLE finance_journal_line
    ADD CONSTRAINT ck_finance_journal_exchange_rate
        CHECK (exchange_rate_snapshot IS NULL OR exchange_rate_snapshot > 0);

CREATE TABLE finance_journal_posting_snapshot (
    journal_entry_id uuid PRIMARY KEY
        REFERENCES finance_journal_entry(id),
    transaction_currency char(3) NOT NULL,
    base_currency char(3) NOT NULL,
    exchange_rate_id uuid,
    exchange_rate_source varchar(120) NOT NULL,
    exchange_rate_type varchar(40) NOT NULL,
    exchange_rate_date date NOT NULL,
    exchange_rate numeric(24,12) NOT NULL,
    transaction_amount numeric(24,6) NOT NULL,
    base_amount numeric(24,6) NOT NULL,
    tax_context jsonb NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT ck_finance_snapshot_currency
        CHECK (
            transaction_currency ~ '^[A-Z]{3}$'
            AND base_currency ~ '^[A-Z]{3}$'
        ),
    CONSTRAINT ck_finance_snapshot_rate CHECK (exchange_rate > 0),
    CONSTRAINT ck_finance_snapshot_amounts
        CHECK (transaction_amount >= 0 AND base_amount >= 0),
    CONSTRAINT ck_finance_snapshot_base_rate
        CHECK (transaction_currency <> base_currency OR exchange_rate = 1),
    CONSTRAINT ck_finance_snapshot_conversion
        CHECK (round(transaction_amount * exchange_rate, 6) = base_amount),
    CONSTRAINT ck_finance_snapshot_tax_object
        CHECK (jsonb_typeof(tax_context) = 'object')
);

CREATE TABLE finance_document_number_counter (
    document_type varchar(40) NOT NULL,
    company_id uuid NOT NULL REFERENCES company(id),
    scope_branch_id uuid NOT NULL,
    fiscal_year_id uuid NOT NULL REFERENCES finance_fiscal_year(id),
    next_value bigint NOT NULL,
    PRIMARY KEY (document_type, company_id, scope_branch_id, fiscal_year_id),
    CONSTRAINT ck_finance_number_counter_positive CHECK (next_value > 0)
);

CREATE TABLE finance_document_number_assignment (
    document_id uuid PRIMARY KEY,
    idempotency_key varchar(160) NOT NULL UNIQUE,
    document_type varchar(40) NOT NULL,
    company_id uuid NOT NULL REFERENCES company(id),
    branch_id uuid,
    fiscal_year_id uuid NOT NULL REFERENCES finance_fiscal_year(id),
    assigned_number varchar(160) NOT NULL,
    assigned_at timestamptz NOT NULL,
    CONSTRAINT uq_finance_document_number
        UNIQUE (document_type, company_id, fiscal_year_id, assigned_number),
    CONSTRAINT fk_finance_number_branch_scope
        FOREIGN KEY (branch_id, company_id) REFERENCES branch(id, company_id)
);

CREATE INDEX ix_finance_period_state_dates
    ON finance_accounting_period(fiscal_year_id, period_state, starts_on, ends_on);

CREATE INDEX ix_finance_journal_company_status_date
    ON finance_journal_entry(company_id, status, posting_date);

CREATE OR REPLACE FUNCTION finance_assert_balanced_posted_journal(target_journal uuid)
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
    journal_status varchar(20);
    line_count integer;
    debit_total numeric(24,6);
    credit_total numeric(24,6);
BEGIN
    SELECT status
      INTO journal_status
      FROM finance_journal_entry
     WHERE id = target_journal;
    IF journal_status IN ('POSTED', 'REVERSED') THEN
        SELECT count(*), COALESCE(sum(debit), 0), COALESCE(sum(credit), 0)
          INTO line_count, debit_total, credit_total
          FROM finance_journal_line
         WHERE journal_id = target_journal;
        IF line_count < 2 OR debit_total <> credit_total THEN
            RAISE EXCEPTION 'posted finance journal must contain balanced double-entry lines';
        END IF;
    END IF;
END;
$$;

CREATE OR REPLACE FUNCTION finance_check_journal_balance()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    PERFORM finance_assert_balanced_posted_journal(NEW.id);
    RETURN NULL;
END;
$$;

CREATE OR REPLACE FUNCTION finance_check_journal_line_balance()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    PERFORM finance_assert_balanced_posted_journal(
        CASE WHEN TG_OP = 'DELETE' THEN OLD.journal_id ELSE NEW.journal_id END
    );
    RETURN NULL;
END;
$$;

CREATE CONSTRAINT TRIGGER trg_finance_journal_balance
AFTER INSERT OR UPDATE ON finance_journal_entry
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION finance_check_journal_balance();

CREATE CONSTRAINT TRIGGER trg_finance_journal_line_balance
AFTER INSERT OR UPDATE OR DELETE ON finance_journal_line
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION finance_check_journal_line_balance();

CREATE OR REPLACE FUNCTION finance_reject_snapshot_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'finance posting snapshots are immutable';
END;
$$;

CREATE TRIGGER trg_finance_snapshot_immutable
BEFORE UPDATE OR DELETE ON finance_journal_posting_snapshot
FOR EACH ROW
EXECUTE FUNCTION finance_reject_snapshot_mutation();

INSERT INTO iam_permission (id, capability, description)
VALUES
    ('3c000000-0000-4000-8000-000000000001', 'finance.account.create',
     'Create company-scoped General Ledger accounts'),
    ('3c000000-0000-4000-8000-000000000002', 'finance.fiscal-year.create',
     'Create company-scoped fiscal years'),
    ('3c000000-0000-4000-8000-000000000003', 'finance.period.create',
     'Create company-scoped accounting periods'),
    ('3c000000-0000-4000-8000-000000000004', 'finance.period.manage',
     'Close or reopen company-scoped accounting periods'),
    ('3c000000-0000-4000-8000-000000000005', 'finance.journal.create',
     'Create company-scoped General Ledger journals'),
    ('3c000000-0000-4000-8000-000000000006', 'finance.journal.edit',
     'Edit company-scoped draft General Ledger journals'),
    ('3c000000-0000-4000-8000-000000000007', 'finance.journal.post',
     'Post company-scoped General Ledger journals'),
    ('3c000000-0000-4000-8000-000000000008', 'finance.journal.reverse',
     'Reverse company-scoped posted General Ledger journals')
ON CONFLICT (capability) DO NOTHING;

INSERT INTO platform_domain_event_catalog (event_type, owner_context, description)
VALUES
    ('FinanceAccountingPeriodOPEN', 'finance',
     'An accounting period was reopened under Finance authorization'),
    ('FinanceAccountingPeriodCLOSING', 'finance',
     'An accounting period entered controlled closing'),
    ('FinanceAccountingPeriodCLOSED', 'finance',
     'An accounting period was closed'),
    ('FinanceJournalPosted', 'finance',
     'An immutable balanced General Ledger journal was posted'),
    ('FinanceJournalReversed', 'finance',
     'A posted General Ledger journal received a compensating reversal')
ON CONFLICT (event_type) DO NOTHING;
