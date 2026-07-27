ALTER TABLE finance_journal_posting_snapshot
    ADD COLUMN posted_at timestamptz;

UPDATE finance_journal_posting_snapshot snapshot
SET posted_at = journal.created_at
FROM finance_journal_entry journal
WHERE journal.id = snapshot.journal_entry_id;

ALTER TABLE finance_journal_posting_snapshot
    ALTER COLUMN posted_at SET NOT NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM finance_journal_entry journal
        WHERE journal.status = 'POSTED'
          AND NOT EXISTS (
              SELECT 1
              FROM finance_journal_posting_snapshot snapshot
              WHERE snapshot.journal_entry_id = journal.id
          )
    ) THEN
        RAISE EXCEPTION 'existing posted finance journal lacks an authoritative posting snapshot';
    END IF;
END;
$$;

CREATE OR REPLACE FUNCTION finance_require_posting_snapshot()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.status = 'POSTED'
       AND NOT EXISTS (
           SELECT 1
           FROM finance_journal_posting_snapshot snapshot
           WHERE snapshot.journal_entry_id = NEW.id
       ) THEN
        RAISE EXCEPTION 'posted finance journal requires an authoritative posting snapshot';
    END IF;
    RETURN NULL;
END;
$$;

CREATE CONSTRAINT TRIGGER trg_finance_journal_requires_snapshot
AFTER INSERT OR UPDATE ON finance_journal_entry
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION finance_require_posting_snapshot();

INSERT INTO iam_permission (id, capability, description)
VALUES (
    '3c000000-0000-4000-8000-000000000009',
    'finance.journal.close-adjustment.post',
    'Post authorized General Ledger close adjustments in a closing period'
)
ON CONFLICT (capability) DO NOTHING;
