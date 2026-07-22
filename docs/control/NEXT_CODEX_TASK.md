# Next Codex Task

## Recommended Task: Complete P3.7 Finance Foundation Implementation and Review

P3.6 Sales Foundation is approved and merged. P3.7 Finance Foundation is the only approved active
implementation scope. Accounts Payable, Accounts Receivable, payments, banking, tax filing, fixed
assets, budgeting, payroll, consolidation, automatic posting, and reporting remain out of scope.

The next task is implementation, validation, and approval handling for Finance Foundation only.

## Completed P3.6 Scope

Review and validate implementation for:

- Customers, contacts, addresses, statuses, credit profiles, and customer product/SKU references.
- Sales quotations, lines, approval, controlled revision, commercial-term preservation, and expiry
  rules.
- Sales orders, approvals, amendments, cancellations, requested delivery schedule, partial
  reservation and delivery tracking, and remaining-quantity consistency.
- Master Data, Shared Product Catalog, Enterprise Structure, Identity, Platform audit/events,
  attachments, number series, and explicit Inventory availability/reservation/delivery-request port
  reuse.

## P3.6 Release Boundaries

- Do not implement Accounts Receivable, accounting journal entries, customer payments, credit
  collection, pricing engine, discount engine, direct inventory balance mutation, delivery
  execution, stock issue posting, invoicing, CRM campaigns, Manufacturing, HR, forecasting, or
  automated replenishment.
- Keep Sales behind domain, application, API, and infrastructure layers.
- Keep persistence limited to approved Sales tables.
- Do not begin Accounts Receivable, Accounting, Pricing, CRM, or any next phase without explicit
  architectural approval.

## P3.6 Completion Record

- Sales domain model, repository port, repository adapter, service, REST API, DTOs, validation,
  Flyway migration, unit tests, integration tests, and architecture tests passed review.
- API, persistence, migrations, architecture checks, security checks, and tests passed quality
  gates.
- No operational business modules beyond the approved Sales Foundation were implemented.
