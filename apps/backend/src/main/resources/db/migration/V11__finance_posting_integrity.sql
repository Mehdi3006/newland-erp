ALTER TABLE finance_posting_request
    ADD CONSTRAINT fk_finance_posting_request_journal
    FOREIGN KEY (journal_entry_id) REFERENCES finance_journal_entry(id);

CREATE SEQUENCE finance_journal_number_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE finance_financial_dimension (
    id uuid PRIMARY KEY,
    company_id uuid NOT NULL REFERENCES company(id),
    code varchar(100) NOT NULL,
    active boolean NOT NULL,
    CONSTRAINT uq_finance_financial_dimension UNIQUE (company_id, code)
);

CREATE INDEX ix_finance_financial_dimension_active
    ON finance_financial_dimension(company_id, active);

ALTER TABLE finance_account
    ADD CONSTRAINT fk_finance_account_company FOREIGN KEY (company_id) REFERENCES company(id);
ALTER TABLE finance_chart_of_accounts
    ADD CONSTRAINT fk_finance_chart_company FOREIGN KEY (company_id) REFERENCES company(id);
ALTER TABLE finance_fiscal_year
    ADD CONSTRAINT fk_finance_fiscal_year_company FOREIGN KEY (company_id) REFERENCES company(id);
ALTER TABLE finance_cost_center
    ADD CONSTRAINT fk_finance_cost_center_company FOREIGN KEY (company_id) REFERENCES company(id);
ALTER TABLE finance_profit_center
    ADD CONSTRAINT fk_finance_profit_center_company FOREIGN KEY (company_id) REFERENCES company(id);
ALTER TABLE finance_journal_entry
    ADD CONSTRAINT fk_finance_journal_company FOREIGN KEY (company_id) REFERENCES company(id),
    ADD CONSTRAINT fk_finance_journal_branch_scope
        FOREIGN KEY (branch_id, company_id) REFERENCES branch(id, company_id);
ALTER TABLE finance_journal_line
    ADD CONSTRAINT fk_finance_journal_line_cost_center
        FOREIGN KEY (cost_center_id) REFERENCES finance_cost_center(id),
    ADD CONSTRAINT fk_finance_journal_line_profit_center
        FOREIGN KEY (profit_center_id) REFERENCES finance_profit_center(id);
ALTER TABLE finance_accounting_event
    ADD CONSTRAINT fk_finance_accounting_event_company
        FOREIGN KEY (company_id) REFERENCES company(id),
    ADD CONSTRAINT fk_finance_accounting_event_branch_scope
        FOREIGN KEY (branch_id, company_id) REFERENCES branch(id, company_id);
ALTER TABLE finance_posting_rule
    ADD CONSTRAINT fk_finance_posting_rule_company FOREIGN KEY (company_id) REFERENCES company(id);
ALTER TABLE finance_posting_rule_line
    ADD CONSTRAINT ck_finance_posting_rule_line_direction CHECK (direction IN ('DEBIT', 'CREDIT')),
    ADD CONSTRAINT ck_finance_posting_rule_line_account_resolution
        CHECK (account_resolution_type IN ('FIXED_ACCOUNT', 'EVENT_ATTRIBUTE_ACCOUNT')),
    ADD CONSTRAINT ck_finance_posting_rule_line_amount_expression
        CHECK (amount_expression IN (
            'EVENT_AMOUNT', 'EVENT_TAX_AMOUNT', 'EVENT_NET_AMOUNT', 'EVENT_COST_AMOUNT', 'CONSTANT'
        ));
ALTER TABLE finance_posting_request
    ADD CONSTRAINT ck_finance_posting_request_status CHECK (status IN (
        'RECEIVED', 'VALIDATING', 'RULE_RESOLVED', 'JOURNAL_CREATED', 'POSTED', 'FAILED', 'REJECTED'
    ));

ALTER TABLE finance_posting_rule_line
    ADD CONSTRAINT fk_finance_posting_rule_line_account
    FOREIGN KEY (fixed_account_id) REFERENCES finance_account(id);

CREATE INDEX ix_finance_posting_request_status
    ON finance_posting_request(status, updated_at);

CREATE INDEX ix_finance_posting_rule_line_account
    ON finance_posting_rule_line(fixed_account_id)
    WHERE fixed_account_id IS NOT NULL;

ALTER TABLE finance_posting_rule
    ADD CONSTRAINT ck_finance_posting_rule_priority CHECK (priority >= 0),
    ADD CONSTRAINT ck_finance_posting_rule_version CHECK (version >= 1),
    ADD CONSTRAINT ck_finance_posting_rule_status
        CHECK (status IN ('DRAFT', 'ACTIVE', 'RETIRED'));

CREATE UNIQUE INDEX uq_finance_posting_rule_global_version
    ON finance_posting_rule(code, version)
    WHERE company_id IS NULL;

CREATE OR REPLACE FUNCTION reject_finance_accounting_event_update()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Accepted accounting events are immutable';
END;
$$;

CREATE TRIGGER trg_finance_accounting_event_immutable
BEFORE UPDATE OR DELETE ON finance_accounting_event
FOR EACH ROW EXECUTE FUNCTION reject_finance_accounting_event_update();

CREATE OR REPLACE FUNCTION reject_posted_posting_request_change()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.status = 'POSTED' THEN
        RAISE EXCEPTION 'Posted posting requests are immutable';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_finance_posting_request_terminal
BEFORE UPDATE OR DELETE ON finance_posting_request
FOR EACH ROW EXECUTE FUNCTION reject_posted_posting_request_change();

CREATE OR REPLACE FUNCTION enforce_posting_rule_lifecycle()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.code IS DISTINCT FROM NEW.code
        OR OLD.name IS DISTINCT FROM NEW.name
        OR OLD.event_type IS DISTINCT FROM NEW.event_type
        OR OLD.company_id IS DISTINCT FROM NEW.company_id
        OR OLD.effective_from IS DISTINCT FROM NEW.effective_from
        OR OLD.effective_to IS DISTINCT FROM NEW.effective_to
        OR OLD.priority IS DISTINCT FROM NEW.priority
        OR OLD.version IS DISTINCT FROM NEW.version
        OR OLD.created_at IS DISTINCT FROM NEW.created_at
        OR OLD.created_by IS DISTINCT FROM NEW.created_by THEN
        RAISE EXCEPTION 'Posting rule versions are immutable';
    END IF;
    IF NOT (
        (OLD.status = 'DRAFT' AND NEW.status = 'ACTIVE')
        OR (OLD.status = 'ACTIVE' AND NEW.status = 'RETIRED')
    ) THEN
        RAISE EXCEPTION 'Invalid posting rule lifecycle transition';
    END IF;
    IF NEW.updated_at IS NULL OR NEW.updated_by IS NULL THEN
        RAISE EXCEPTION 'Posting rule lifecycle audit data is required';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_finance_posting_rule_lifecycle
BEFORE UPDATE ON finance_posting_rule
FOR EACH ROW EXECUTE FUNCTION enforce_posting_rule_lifecycle();

CREATE OR REPLACE FUNCTION reject_posting_rule_delete()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Posting rule versions cannot be deleted';
END;
$$;

CREATE TRIGGER trg_finance_posting_rule_delete
BEFORE DELETE ON finance_posting_rule
FOR EACH ROW EXECUTE FUNCTION reject_posting_rule_delete();

CREATE OR REPLACE FUNCTION reject_posting_rule_line_change()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Posting rule version lines are immutable';
END;
$$;

CREATE TRIGGER trg_finance_posting_rule_line_immutable
BEFORE UPDATE OR DELETE ON finance_posting_rule_line
FOR EACH ROW EXECUTE FUNCTION reject_posting_rule_line_change();

CREATE OR REPLACE FUNCTION enforce_finance_posting_request_transition()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.status = NEW.status AND NEW.status = 'VALIDATING' THEN
        RETURN NEW;
    END IF;
    IF (OLD.status = 'RECEIVED' AND NEW.status IN ('VALIDATING', 'FAILED', 'REJECTED'))
       OR (OLD.status = 'VALIDATING' AND NEW.status IN ('RULE_RESOLVED', 'FAILED', 'REJECTED'))
       OR (OLD.status = 'RULE_RESOLVED' AND NEW.status IN ('JOURNAL_CREATED', 'FAILED', 'REJECTED'))
       OR (OLD.status = 'JOURNAL_CREATED' AND NEW.status IN ('POSTED', 'FAILED'))
       OR (OLD.status = 'FAILED' AND NEW.status IN ('VALIDATING', 'FAILED', 'REJECTED')) THEN
        RETURN NEW;
    END IF;
    RAISE EXCEPTION 'Invalid posting request transition: % to %', OLD.status, NEW.status;
END;
$$;

CREATE TRIGGER trg_finance_posting_request_transition
BEFORE UPDATE ON finance_posting_request
FOR EACH ROW EXECUTE FUNCTION enforce_finance_posting_request_transition();

CREATE OR REPLACE FUNCTION reject_posted_finance_journal_change()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.status = 'POSTED' THEN
        RAISE EXCEPTION 'Posted journals are immutable';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_finance_journal_immutable
BEFORE UPDATE OR DELETE ON finance_journal_entry
FOR EACH ROW EXECUTE FUNCTION reject_posted_finance_journal_change();

CREATE OR REPLACE FUNCTION reject_posted_finance_journal_line_change()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    affected_journal_id uuid;
BEGIN
    affected_journal_id := CASE WHEN TG_OP = 'DELETE' THEN OLD.journal_id ELSE NEW.journal_id END;
    IF EXISTS (
        SELECT 1 FROM finance_journal_entry
        WHERE id = affected_journal_id AND status = 'POSTED'
    ) THEN
        RAISE EXCEPTION 'Posted journal lines are immutable';
    END IF;
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_finance_journal_line_immutable
BEFORE INSERT OR UPDATE OR DELETE ON finance_journal_line
FOR EACH ROW EXECUTE FUNCTION reject_posted_finance_journal_line_change();
