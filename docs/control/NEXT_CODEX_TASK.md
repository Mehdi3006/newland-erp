# Next Codex Task

## Recommended Task: Complete P3.2 Identity and Access Review

P3.1 is approved and merged. P3.2 implementation exists on branch `codex/p3-2-identity-access` and
must receive explicit architectural approval before any Inventory or other business-module work
begins.

The next task is final review, validation, and approval handling for Identity and Access only.

## P3.2 Scope

Review and validate implementation for:

- Users.
- Roles.
- Permissions and capabilities.
- User-role assignment.
- Role-permission assignment.
- Organization scopes.
- Authentication.
- JWT access tokens.
- Refresh-token rotation.
- Password management.
- Sessions and revocation.
- Identity administration UI.

## P3.2 Rules

- Do not implement Inventory, Product, Procurement, Sales, Accounting, CRM, HR, Payroll, Reporting,
  Workflow, Integration, or AI in P3.2.
- Do not model employees or HR records in Identity and Access.
- Do not begin Inventory before P3.2 approval.

## P3.2 Acceptance Draft

- Identity and Access aggregate model passes review.
- Authentication and refresh-token rotation pass review.
- Capability and organization-scope authorization pass review.
- Password hashing, password policy, account lock, and session revocation pass review.
- API, persistence, migrations, architecture checks, security checks, and UI contract tests pass
  quality gates.
- No business modules outside Identity and Access implemented.
