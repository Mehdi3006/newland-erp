# Current System State

Status: Phase P3.4 Inventory Foundation implementation on branch `codex/p3-4-inventory-foundation`.

## Phase State

- P1 Repository Foundation is approved, closed, and frozen.
- P2 Business Architecture is approved, closed, and frozen.
- P3.1 Enterprise Structure is approved, merged, and part of `main`.
- P3.2 Identity and Access is approved, merged, and part of `main`.
- P3.2.5 Platform Foundation is approved, merged, and part of `main`.
- P3.3 Master Data is approved, merged, and part of `main`.
- P3.3.5 Shared Product Catalog is approved, merged, and part of `main`.
- P3.4 Inventory Foundation implementation is present on this branch and remains under quality and
  architecture review until explicitly approved.
- Procurement, Sales, Pricing, Accounting, CRM, HR, Manufacturing, and other adjacent ERP modules
  have not started and must not begin until explicitly approved.

## Current Repository Content

- Repository foundation documents and tooling from P1.
- Business architecture documentation under `docs/business-architecture`.
- Control documents under `docs/control`.
- Enterprise Structure backend slice for P3.1 under
  `apps/backend/src/main/java/com/newland/erp/enterprise`.
- Enterprise Structure Flyway foundation migration under
  `apps/backend/src/main/resources/db/migration`.
- Enterprise Structure API and persistence tests under
  `apps/backend/src/test/java/com/newland/erp/enterprise`.
- Static Enterprise Structure API contract page under `apps/web/enterprise-structure`.
- Identity and Access backend slice for P3.2 under
  `apps/backend/src/main/java/com/newland/erp/identity`.
- Identity and Access Flyway foundation migration under
  `apps/backend/src/main/resources/db/migration/V2__identity_access_foundation.sql`.
- Identity and Access tests under `apps/backend/src/test/java/com/newland/erp/identity`.
- Static Identity and Access administration page under `apps/web/identity-access`.
- Platform Foundation backend slice for P3.2.5 under
  `apps/backend/src/main/java/com/newland/erp/platform`.
- Platform Foundation Flyway migration under
  `apps/backend/src/main/resources/db/migration/V3__platform_foundation.sql`.
- Platform Foundation tests under `apps/backend/src/test/java/com/newland/erp/platform`.
- Master Data backend slice for P3.3 under `apps/backend/src/main/java/com/newland/erp/masterdata`.
- Master Data Flyway migration under
  `apps/backend/src/main/resources/db/migration/V4__master_data_foundation.sql`.
- Master Data tests under `apps/backend/src/test/java/com/newland/erp/masterdata`.
- Shared Product Catalog backend slice for P3.3.5 under
  `apps/backend/src/main/java/com/newland/erp/productcatalog`.
- Shared Product Catalog Flyway migration under
  `apps/backend/src/main/resources/db/migration/V5__shared_product_catalog_foundation.sql`.
- Shared Product Catalog tests under `apps/backend/src/test/java/com/newland/erp/productcatalog`.
- Inventory Foundation backend slice for P3.4 under
  `apps/backend/src/main/java/com/newland/erp/inventory`.
- Inventory Foundation Flyway migration under
  `apps/backend/src/main/resources/db/migration/V6__inventory_foundation.sql`.
- Inventory Foundation tests under `apps/backend/src/test/java/com/newland/erp/inventory`.

## Current Business Architecture Baseline

- Capability map defined.
- Bounded contexts defined.
- Context map and ownership matrices defined.
- Organization model defined.
- Master-data architecture defined.
- Main end-to-end process maps defined.
- Ubiquitous language defined.
- Event catalog defined.
- Permission model defined.
- Numbering and status architecture defined.
- Reporting and integration maps defined.
- Open decisions centralized.

## Implementation State

P3.4 contains only the inventory foundation. It adds stock transaction posting, stock movement
lines, append-only stock ledger entries, derived stock balances, reservations and releases, lots,
serial numbers, inventory statuses, expiry-date checks, reversal transactions, idempotency
protection, optimistic/database-locking repository boundaries, audit integration, domain event and
outbox integration ports, attachment reuse, number-series reuse, and identity authorization reuse.
No procurement workflow, purchase order, sales order, pricing, accounting journal entry, costing
method, manufacturing, CRM, HR, advanced replenishment, forecasting, fake operational data, or
unrelated ERP implementation exists in this branch.
