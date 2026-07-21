# Next Codex Task

## Recommended Task: Complete P3.6 Sales Foundation Implementation and Review

P3.5 Procurement Foundation is approved and merged. P3.6 implementation exists on branch
`codex/p3-6-sales-foundation` and must receive explicit architectural approval before any Accounts
Receivable, Accounting, customer payment, Pricing, Discount, CRM, HR, Manufacturing, Forecasting,
Automated Replenishment, or adjacent operational ERP module work begins.

The next task is implementation, validation, and approval handling for Sales Foundation only.

## P3.6 Scope

Review and validate implementation for:

- Customers, contacts, addresses, statuses, credit profiles, and customer product/SKU references.
- Sales quotations, lines, approval, controlled revision, commercial-term preservation, and expiry
  rules.
- Sales orders, approvals, amendments, cancellations, requested delivery schedule, partial
  reservation and delivery tracking, and remaining-quantity consistency.
- Master Data, Shared Product Catalog, Enterprise Structure, Identity, Platform audit/events,
  attachments, number series, and explicit Inventory availability/reservation/delivery-request port
  reuse.

## P3.6 Rules

- Do not implement Accounts Receivable, accounting journal entries, customer payments, credit
  collection, pricing engine, discount engine, direct inventory balance mutation, delivery
  execution, stock issue posting, invoicing, CRM campaigns, Manufacturing, HR, forecasting, or
  automated replenishment.
- Keep Sales behind domain, application, API, and infrastructure layers.
- Keep persistence limited to approved Sales tables.
- Do not begin Accounts Receivable, Accounting, Pricing, CRM, or any next phase before P3.6
  approval.

## P3.6 Acceptance Draft

- Sales domain model, repository port, repository adapter, service, REST API, DTOs, validation,
  Flyway migration, unit tests, integration tests, and architecture tests pass review.
- API, persistence, migrations, architecture checks, security checks, and tests pass quality gates.
- No operational business modules implemented.
