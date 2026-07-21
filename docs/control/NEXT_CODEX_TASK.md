# Next Codex Task

## Recommended Task: Complete P3.4 Inventory Foundation Review

P3.3.5 Shared Product Catalog is approved and merged. P3.4 implementation exists on branch
`codex/p3-4-inventory-foundation` and must receive explicit architectural approval before any
Procurement, Sales, Pricing, Accounting, CRM, HR, Manufacturing, or adjacent operational ERP module
work begins.

The next task is final review, validation, and approval handling for Inventory Foundation only.

## P3.4 Scope

Review and validate implementation for:

- Stock transactions, movement lines, movement types, opening balances, receipts, issues, transfers,
  adjustments, reversals, idempotency protection, and posted transaction immutability.
- Append-only stock ledger entries and derived stock balances for on-hand, reserved, available,
  in-transit, damaged, and quarantine quantities.
- Reservations and releases that affect available quantity without directly changing on-hand.
- Lot, serial-number, expiry-date, quarantine, damaged, and inventory-status restrictions.
- Audit, domain event/outbox, attachment, number-series, identity authorization, product/SKU, UOM,
  warehouse, zone, and bin reference reuse.

## P3.4 Rules

- Do not implement procurement workflows, purchase orders, sales orders, pricing, accounting journal
  entries, costing methods, manufacturing, CRM, HR, advanced replenishment, forecasting, or
  supplier-specific purchasing data.
- Keep Inventory behind domain, application, API, and infrastructure layers.
- Keep persistence limited to approved Inventory tables.
- Do not begin Procurement, Sales, Pricing, Accounting, or any next phase before P3.4 approval.

## P3.4 Acceptance Draft

- Inventory domain model, repository port, repository adapter, service, REST API, DTOs, validation,
  Flyway migration, unit tests, integration tests, and architecture tests pass review.
- API, persistence, migrations, architecture checks, security checks, and tests pass quality gates.
- No operational business modules implemented.
