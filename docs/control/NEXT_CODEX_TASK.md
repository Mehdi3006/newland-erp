# Next Codex Task

## Recommended Task: Complete P3.3.5 Shared Product Catalog Review

P3.3 Master Data is approved and merged. P3.3.5 implementation exists on branch
`codex/p3-3-5-shared-product-catalog` and must receive explicit architectural approval before any
Inventory, Procurement, Sales, Pricing, or other operational ERP module work begins.

The next task is final review, validation, and approval handling for Shared Product Catalog only.

## P3.3.5 Scope

Review and validate implementation for:

- Product, SKU, product code, GTIN/EAN/UPC, barcode, category assignment, brand assignment, family
  assignment, attributes, and attribute values.
- UOM assignment, packaging hierarchy, units per package, dimensions, and weight metadata.
- Product media, images, documents, manuals, brochures, localized content, tags, search metadata,
  warranty metadata, and lifecycle status.
- Audit support plus attachment and localization integration ports.

## P3.3.5 Rules

- Do not implement inventory balances, stock movements, warehouse operations, procurement, sales,
  pricing, accounting, CRM, HR, manufacturing, or supplier-specific purchasing data.
- Keep Product Catalog behind domain, application, API, and infrastructure layers.
- Keep persistence limited to approved Product Catalog tables.
- Do not begin Inventory or operational Product Information Management before P3.3.5 approval.

## P3.3.5 Acceptance Draft

- Shared Product Catalog domain model, repository port, repository adapter, service, REST API, DTOs,
  validation, Flyway migration, unit tests, integration tests, and architecture tests pass review.
- API, persistence, migrations, architecture checks, security checks, and tests pass quality gates.
- No operational business modules implemented.
