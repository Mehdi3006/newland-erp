# Next Codex Task

## Recommended Task: P3.1 Enterprise Structure Implementation Plan

Do not start coding until P2 is approved.

After P2 approval, the next task should be documentation-to-implementation planning for Enterprise
Structure only.

## P3.1 Scope

Prepare implementation design for:

- Enterprise.
- Legal Entity.
- Company.
- Branch.
- Warehouse.
- Zone.
- Location.
- Project.
- Department.
- Cost Center.
- Profit Center.
- Sales Region.
- Service Center.

## P3.1 Rules

- Resolve or explicitly defer legal/company/branch/warehouse open decisions.
- Define aggregates and invariants before code.
- Define permission scopes before API or UI.
- Define persistence model before migrations.
- Do not implement Product, Inventory, Procurement, Sales, Accounting, CRM, Service, HR, Payroll,
  Reporting, Integration, or AI in P3.1.

## P3.1 Acceptance Draft

- Enterprise Structure aggregate model approved.
- Scope inheritance rules approved.
- Activation/deactivation state model approved.
- Company switching and visibility rules approved.
- Inter-company transaction requirements documented.
- Permission scope model approved.
- No business modules outside Enterprise Structure implemented.
