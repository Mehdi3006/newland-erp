ALTER TABLE platform_outbox
    DROP CONSTRAINT ck_platform_outbox_status,
    ADD CONSTRAINT ck_platform_outbox_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED'));

DROP INDEX ix_platform_outbox_pending;
CREATE INDEX ix_platform_outbox_dispatch
    ON platform_outbox (status, next_attempt_at, created_at)
    WHERE status IN ('PENDING', 'PROCESSING', 'FAILED');
