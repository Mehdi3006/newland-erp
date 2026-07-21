# Next Codex Task

## Recommended Task: Complete P3.3 Master Data Review

P3.2.5 is approved and merged. P3.3 implementation exists on branch `codex/p3-3-master-data` and
must receive explicit architectural approval before any operational ERP module work begins.

The next task is final review, validation, and approval handling for Master Data only.

## P3.3 Scope

Review and validate implementation for:

- Organization, company, business unit, branch, warehouse, warehouse zone, and warehouse bin
  reference data.
- Currency, exchange rate, country, province, city, address, language, and time-zone reference data.
- Unit of measure and unit-conversion reference data.
- Tax category, tax rate, payment terms, payment method, shipping method, and Incoterms reference
  data.
- Fiscal calendar, number series, document type, attachment category, product category, product
  brand, product family, product attribute, attribute value, and barcode type reference data.

## P3.3 Rules

- Do not implement inventory quantities, stock movements, procurement, sales, accounting, CRM, HR,
  pricing, or manufacturing in P3.3.
- Keep Master Data behind domain, application, API, and infrastructure layers.
- Keep persistence limited to approved Master Data tables.
- Do not begin P3.3.x Product Information Management or Inventory before P3.3 approval.

## P3.3 Acceptance Draft

- Master Data domain model, repository port, repository adapter, service, REST API, DTOs,
  validation, Flyway migration, integration tests, and architecture tests pass review.
- API, persistence, migrations, architecture checks, security checks, and tests pass quality gates.
- No operational business modules implemented.
