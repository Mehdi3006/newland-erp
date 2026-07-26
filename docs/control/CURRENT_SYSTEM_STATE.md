# Current System State

Status: Phase P3.11 Service and Warranty is implemented on a feature branch and awaiting
architecture review.

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
- P3.7 Finance Foundation is approved, merged, and part of `main`.
- P3.8 Financial Posting Infrastructure is approved, merged, and part of `main`; no operational
  module is connected to automatic posting.
- P3.9.1 Procurement to Finance Integration is approved, merged, and part of `main`.
- P3.9 Import Logistics is approved, merged, and part of `main`.
- P3.10 CRM is approved, merged, and part of `main`.
- P3.11 Service and Warranty is implemented and awaiting review; it is not yet part of `main`.
- Pricing, Accounts Receivable, Accounting, HR, Manufacturing, and other adjacent ERP modules have
  not started and must not begin until explicitly approved.

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
- Finance Foundation backend slice under `apps/backend/src/main/java/com/newland/erp/finance`.
- Finance Foundation Flyway migration under
  `apps/backend/src/main/resources/db/migration/V9__finance_foundation.sql`.
- Financial Posting Infrastructure under
  `apps/backend/src/main/java/com/newland/erp/finance/posting`.
- Financial Posting Infrastructure Flyway migrations under
  `apps/backend/src/main/resources/db/migration/V10__finance_posting_infrastructure.sql`,
  `V11__finance_posting_integrity.sql`, `V12__finance_posting_release_guards.sql`, and
  `V13__release_blocker_guards.sql`.
- Financial Posting Infrastructure unit, PostgreSQL integration, concurrency, rollback, security,
  audit, outbox, and architecture tests under `apps/backend/src/test`.
- Procurement to Finance integration through the Finance published posting API, with company-scoped
  authorization, audit/outbox reuse, a purchase-order feature flag, and forward-only migration
  `V14__procurement_finance_integration.sql`.
- Import Logistics bounded context for approved-PO shipments, containers, customs milestones, and
  landed-cost drafts under `apps/backend/src/main/java/com/newland/erp/logistics`.
- CRM bounded context for leads, opportunities, activities, and customer timelines under
  `apps/backend/src/main/java/com/newland/erp/crm`.
- Service and Warranty bounded context under
  `apps/backend/src/main/java/com/newland/erp/servicewarranty`.

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

P3.11 adds configurable warranty policies, service-ticket lifecycle control, validation against
Sales-owned customer and delivered-order evidence, Product Catalog references, Inventory serial
references, diagnosis, repair/replacement decisions, and documented closure. It does not issue
inventory, post accounting, invoice service, schedule technicians, or implement a mobile workflow.

P3.10 adds only the CRM engagement foundation: leads, qualification/disqualification, opportunity
conversion and controlled stages, immutable activities, and company-scoped customer timelines. Sales
remains the customer master owner and exposes customer references through a published port. CRM adds
no campaigns, marketing automation, quotation/order execution, pricing, invoicing, or service
functionality.

P3.9.1 connects only five Procurement accounting facts to the Finance Posting Engine:
PurchaseOrderApproved (feature flagged), GoodsReceived, SupplierInvoicePosted,
SupplierCreditNotePosted, and SupplierPaymentPosted. Procurement contains no accounting rules,
creates no journal directly, and does not access Finance persistence. Inventory, Sales,
Manufacturing, and Assets are unchanged.

P3.9 adds import shipment booking, container loading, customs tracking, and landed-cost drafts. It
does not receive inventory, post landed cost, call external carriers/customs services, or add
accounting rules.

P3.8 adds only explicit financial-posting infrastructure: immutable accounting events, versioned
posting rules, durable and idempotent posting requests, real Finance journal creation/posting,
transactional audit/outbox records, and retry/concurrency guards. It does not connect Procurement,
Sales, Inventory, or any other source module to automatic posting.

P3.7 contains only the Finance Foundation. It adds chart of accounts, account hierarchy, fiscal
years, accounting periods, journals, reversals, cost/profit centers, currency snapshots, and
explicit future finance posting ports. It does not add AP, AR, payments, banking, tax filing,
assets, budgeting, payroll, consolidation, automatic source-document posting, financial statements,
or reporting.

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
