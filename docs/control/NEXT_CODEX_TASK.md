# Next Codex Task

## Recommended Task: Review P3.9.1 Procurement to Finance Integration

P3.9.1 is implemented on a feature branch and must pass architecture review before merge. Review the
Procurement accounting-event contract, published Finance API boundary, company authorization,
idempotency/retry behavior, audit/outbox integration, and PostgreSQL migration.

Do not begin Inventory, Sales, Manufacturing, Assets, or another integration phase.

## P3.9.1 Review Scope

- Five Procurement events only: purchase-order approval behind a disabled-by-default feature flag,
  goods receipt, supplier invoice, supplier credit note, and supplier payment.
- Procurement publishes immutable facts through Finance's named posting integration API.
- Finance retains posting-rule evaluation, journal creation, idempotency, retry, and concurrency
  ownership.
- Identity company scope and Platform audit/outbox capabilities are reused through ports.

## P3.9.1 Release Boundaries

- Do not introduce accounting rules or direct journal access in Procurement.
- Do not implement Inventory, Sales, Manufacturing, Assets, or unrelated operational behavior.
- Keep Finance persistence and internal application/domain types inaccessible to Procurement.
- Do not begin another phase without explicit architectural approval.
