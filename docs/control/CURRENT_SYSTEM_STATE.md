# Current System State

Status: Phase P3.2 Identity and Access implementation review on branch `codex/p3-2-identity-access`.

## Phase State

- P1 Repository Foundation is approved, closed, and frozen.
- P2 Business Architecture is approved, closed, and frozen.
- P3.1 Enterprise Structure is approved, merged, and part of `main`.
- P3.2 Identity and Access implementation is present on this branch and remains under quality and
  architecture review until explicitly approved.
- Inventory and all other ERP modules have not started and must not begin until explicitly approved.

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

P3.2 contains only the Identity and Access foundation: users, roles, permissions, scoped role and
permission assignments, authentication, JWT access tokens, refresh-token rotation, password
management, sessions, and IAM administration UI. No Inventory, Sales, Procurement, Accounting, CRM,
HR, Payroll, Reporting, Workflow, fake operational data, or unrelated ERP module implementation
exists in this branch.
