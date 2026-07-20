# Next Codex Task

## Recommended Task: Complete P3.1 Enterprise Structure Review

P2 is approved. P3.1 implementation exists in PR #11 and must receive explicit architectural
approval before any P3.2 work begins.

The next task is final review, validation, and approval handling for Enterprise Structure only.

## P3.1 Scope

Review and validate implementation for:

- Enterprise.
- Legal Entity.
- Company.
- Branch.
- Warehouse.
- Zone.
- Location.

## P3.1 Rules

- Do not add project, department, cost center, profit center, sales region, or service center
  implementation without a separate approved phase.
- Do not implement Product, Inventory, Procurement, Sales, Accounting, CRM, Service, HR, Payroll,
  Reporting, Integration, or AI in P3.1.
- Do not begin P3.2 Identity and Access before P3.1 approval.

## P3.1 Acceptance Draft

- Enterprise Structure aggregate model passes review.
- Activation/deactivation state model passes review.
- API error semantics, permission checks, persistence, migrations, and tests pass quality gates.
- Project and advanced scope implementation explicitly deferred.
- No business modules outside Enterprise Structure implemented.
