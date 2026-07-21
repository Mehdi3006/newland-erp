# Next Codex Task

## Recommended Task: Complete P3.5 Procurement Foundation Review

P3.4 Inventory Foundation is approved and merged. P3.5 implementation exists on branch
`codex/p3-5-procurement-foundation` and must receive explicit architectural approval before any
Accounts Payable, Accounting, Sales, Pricing, CRM, HR, Manufacturing, Advanced Sourcing, Automated
Replenishment, Demand Forecasting, or adjacent operational ERP module work begins.

The next task is final review, validation, and approval handling for Procurement Foundation only.

## P3.5 Scope

Review and validate implementation for:

- Supplier, contacts, addresses, statuses, supplier product/SKU references, lead time, minimum order
  quantity, and packaging metadata.
- Purchase requisitions, lines, approval, rejection, resubmission, immutability, and controlled
  revision rules.
- RFQs, multiple supplier invitations, supplier quotations, commercial-term preservation, and
  auditable quotation comparison.
- Purchase orders, approvals, amendments, cancellations, expected delivery schedule, partial
  delivery tracking, and remaining-quantity consistency.
- Master Data, Shared Product Catalog, Enterprise Structure, Identity, Platform audit/events,
  attachments, number series, and explicit Inventory receipt-request port reuse.

## P3.5 Rules

- Do not implement Accounts Payable, accounting journal entries, supplier payments, direct inventory
  balance mutation, goods receipt posting logic, Sales, pricing engine, CRM, Manufacturing, HR,
  advanced sourcing, automated replenishment, or demand forecasting.
- Keep Procurement behind domain, application, API, and infrastructure layers.
- Keep persistence limited to approved Procurement tables.
- Do not begin Accounts Payable, Accounting, Sales, Pricing, or any next phase before P3.5 approval.

## P3.5 Acceptance Draft

- Procurement domain model, repository port, repository adapter, service, REST API, DTOs,
  validation, Flyway migration, unit tests, integration tests, and architecture tests pass review.
- API, persistence, migrations, architecture checks, security checks, and tests pass quality gates.
- No operational business modules implemented.
