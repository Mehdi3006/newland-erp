# Next Codex Task

## Recommended Task: Review P3.12.1 General Ledger Core

P3.12.1 is implemented on a feature branch and must pass engineering and architecture review before
merge.

Review accounting-period state transitions, PostgreSQL double-entry enforcement, posted journal and
snapshot immutability, atomic idempotency, optimistic concurrency, reversal safety, company and
branch isolation, Identity authorization, transactional audit/outbox behavior, posting snapshots,
published Finance contract adapters, and Spring Modulith boundaries.

Do not begin P3.12.2, AP, AR, Treasury, or another bounded context until P3.12.1 is approved.

## P3.12.1 Boundaries

- No AP supplier-invoice or payment workflow.
- No AR customer-invoice, collection, aging, or allocation workflow.
- No Treasury, UI, reporting, or financial statements.
- Existing P3.8 Posting Engine remains the only rule-driven journal creator.
- Existing Procurement, Inventory, Sales, and Service/Warranty integrations remain unchanged.
