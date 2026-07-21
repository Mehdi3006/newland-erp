# Current System State

Status: Phase P3.6 Sales Foundation is approved, merged, and part of `main`.

## Phase State

- P1 Repository Foundation is approved, closed, and frozen.
- P2 Business Architecture is approved, closed, and frozen.
- P3.1 Enterprise Structure is approved, merged, and part of `main`.
- P3.2 Identity and Access is approved, merged, and part of `main`.
- P3.2.5 Platform Foundation is approved, merged, and part of `main`.
- P3.3 Master Data is approved, merged, and part of `main`.
- P3.3.5 Shared Product Catalog is approved, merged, and part of `main`.
- P3.4 Inventory Foundation is approved, merged, and part of `main`.
- P3.5 Procurement Foundation is approved, merged, and part of `main`.
- P3.6 Sales Foundation is approved, merged, and part of `main`.
- Pricing, Accounts Receivable, Accounting, CRM, HR, Manufacturing, and other adjacent ERP modules
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
- Procurement Foundation backend slice for P3.5 under
  `apps/backend/src/main/java/com/newland/erp/procurement`.
- Procurement Foundation Flyway migration under
  `apps/backend/src/main/resources/db/migration/V7__procurement_foundation.sql`.
- Procurement Foundation tests under `apps/backend/src/test/java/com/newland/erp/procurement`.
- Sales Foundation backend slice for P3.6 under `apps/backend/src/main/java/com/newland/erp/sales`.
- Sales Foundation Flyway migration under
  `apps/backend/src/main/resources/db/migration/V8__sales_foundation.sql`.
- Sales Foundation tests under `apps/backend/src/test/java/com/newland/erp/sales`.

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

P3.6 contains only the sales foundation. It adds customers, contacts, addresses, customer credit
profiles, customer product references, sales quotations, quotation approval/revision/expiry, sales
orders, approvals, amendments, cancellations, reservation and delivery request tracking, idempotency
protection, explicit inventory availability/reservation/delivery requests through ports,
master-data/catalog/enterprise/identity reuse through ports, audit, attachments, number-series, and
domain-event integration. No Accounts Receivable, accounting journal entry, customer payment, credit
collection, pricing engine, discount engine, direct inventory balance mutation, delivery execution,
stock issue posting, invoicing, CRM campaign, manufacturing, HR, forecasting, automated
replenishment, fake operational data, or unrelated ERP implementation exists. P3.6 Sales Foundation
is complete.
