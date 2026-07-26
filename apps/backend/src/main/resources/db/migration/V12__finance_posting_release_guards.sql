ALTER TABLE finance_posting_rule
    ADD CONSTRAINT uq_finance_posting_rule_id_version
        UNIQUE (posting_rule_id, version);

ALTER TABLE finance_posting_request
    ADD CONSTRAINT fk_finance_posting_request_rule_version
        FOREIGN KEY (resolved_posting_rule_id, resolved_posting_rule_version)
        REFERENCES finance_posting_rule(posting_rule_id, version),
    ADD CONSTRAINT ck_finance_posting_request_attempts CHECK (attempts >= 0),
    ADD CONSTRAINT ck_finance_posting_request_version CHECK (version >= 0),
    ADD CONSTRAINT ck_finance_posting_request_resolution CHECK (
        (status IN ('RULE_RESOLVED', 'JOURNAL_CREATED', 'POSTED')
            AND resolved_posting_rule_id IS NOT NULL
            AND resolved_posting_rule_version IS NOT NULL)
        OR status NOT IN ('RULE_RESOLVED', 'JOURNAL_CREATED', 'POSTED')
    ),
    ADD CONSTRAINT ck_finance_posting_request_journal_state CHECK (
        (status IN ('JOURNAL_CREATED', 'POSTED') AND journal_entry_id IS NOT NULL)
        OR status NOT IN ('JOURNAL_CREATED', 'POSTED')
    ),
    ADD CONSTRAINT ck_finance_posting_request_failure_state CHECK (
        (status IN ('FAILED', 'REJECTED')
            AND failure_code IS NOT NULL
            AND failure_message IS NOT NULL)
        OR (status NOT IN ('FAILED', 'REJECTED')
            AND failure_code IS NULL
            AND failure_message IS NULL)
    );

CREATE INDEX ix_finance_posting_request_rule_version
    ON finance_posting_request(resolved_posting_rule_id, resolved_posting_rule_version)
    WHERE resolved_posting_rule_id IS NOT NULL;
CREATE INDEX ix_finance_posting_request_journal
    ON finance_posting_request(journal_entry_id)
    WHERE journal_entry_id IS NOT NULL;
CREATE INDEX ix_finance_journal_line_cost_center
    ON finance_journal_line(cost_center_id)
    WHERE cost_center_id IS NOT NULL;
CREATE INDEX ix_finance_journal_line_profit_center
    ON finance_journal_line(profit_center_id)
    WHERE profit_center_id IS NOT NULL;

CREATE OR REPLACE FUNCTION enforce_posting_rule_line_guard()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    parent_status varchar(20);
    parent_company_id uuid;
    account_company_id uuid;
BEGIN
    IF TG_OP <> 'INSERT' THEN
        RAISE EXCEPTION 'Posting rule version lines are immutable';
    END IF;

    SELECT status, company_id
      INTO parent_status, parent_company_id
      FROM finance_posting_rule
     WHERE posting_rule_id = NEW.posting_rule_id
     FOR KEY SHARE;

    IF parent_status IS NULL THEN
        RAISE EXCEPTION 'Posting rule does not exist';
    END IF;
    IF parent_status <> 'DRAFT' THEN
        RAISE EXCEPTION 'Posting rule lines may only be added to draft rules';
    END IF;

    IF NEW.fixed_account_id IS NOT NULL THEN
        SELECT company_id
          INTO account_company_id
          FROM finance_account
         WHERE id = NEW.fixed_account_id;
        IF parent_company_id IS NULL THEN
            RAISE EXCEPTION 'Global posting rules cannot reference a company account';
        END IF;
        IF account_company_id IS DISTINCT FROM parent_company_id THEN
            RAISE EXCEPTION 'Posting rule account is outside rule company scope';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER trg_finance_posting_rule_line_immutable ON finance_posting_rule_line;
CREATE TRIGGER trg_finance_posting_rule_line_immutable
BEFORE INSERT OR UPDATE OR DELETE ON finance_posting_rule_line
FOR EACH ROW EXECUTE FUNCTION enforce_posting_rule_line_guard();

CREATE OR REPLACE FUNCTION enforce_finance_journal_line_scope()
RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    journal_company_id uuid;
BEGIN
    SELECT company_id
      INTO journal_company_id
      FROM finance_journal_entry
     WHERE id = NEW.journal_id;

    IF NOT EXISTS (
        SELECT 1 FROM finance_account
         WHERE id = NEW.account_id AND company_id = journal_company_id
    ) THEN
        RAISE EXCEPTION 'Journal account is outside journal company scope';
    END IF;
    IF NEW.cost_center_id IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM finance_cost_center
         WHERE id = NEW.cost_center_id AND company_id = journal_company_id
    ) THEN
        RAISE EXCEPTION 'Journal cost center is outside journal company scope';
    END IF;
    IF NEW.profit_center_id IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM finance_profit_center
         WHERE id = NEW.profit_center_id AND company_id = journal_company_id
    ) THEN
        RAISE EXCEPTION 'Journal profit center is outside journal company scope';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_finance_journal_line_scope
BEFORE INSERT OR UPDATE ON finance_journal_line
FOR EACH ROW EXECUTE FUNCTION enforce_finance_journal_line_scope();
