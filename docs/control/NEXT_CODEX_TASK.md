# Next Codex Task

## Recommended Task: Complete the P3.8 Release Gate

P3.7 Finance Foundation is approved and merged. P3.8 Financial Posting Infrastructure is implemented
on PR #20 and must pass its release gate before merge. No subsequent phase may begin until it
receives explicit architectural approval. Accounts Payable, Accounts Receivable, payments, banking,
tax filing, fixed assets, budgeting, payroll, consolidation, source-module automatic posting, and
reporting remain out of scope.

The next task is to complete the P3.8 release gate and await explicit merge authority.

## Completed P3.8 Scope

- Immutable accounting-event acceptance and durable idempotent posting requests.
- Versioned posting-rule lifecycle, precedence, conflict detection, deterministic evaluation, and
  company/system authorization.
- Real Finance journal creation and posting through the existing Finance application service.
- Durable concurrency/retry handling and transactional audit/outbox persistence.
- Explicit integration contracts for Enterprise Structure, Master Data, Identity, and Platform.

## P3.8 Release Boundaries

- Do not introduce source-module automatic posting.
- Do not implement Accounts Payable, Accounts Receivable, payments, banking, tax filing, fixed
  assets, budgeting, payroll, consolidation, statements, or advanced reporting.
- Keep Finance posting behind domain, application, API, and infrastructure layers.
- Do not begin another phase without explicit architectural approval.

## P3.8 Completion Record

- Financial Posting domain model, ports, durable jOOQ adapter, services, REST API, DTOs, validation,
  Flyway migrations, unit tests, PostgreSQL integration tests, and architecture tests are present.
- No automatic Procurement, Sales, or Inventory posting was introduced.
