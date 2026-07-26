# Next Codex Task

## Recommended Task: Review P3.11 Service and Warranty

P3.11 is implemented on a feature branch and must pass architecture review before merge.

Review service-ticket lifecycle invariants, configurable warranty-policy precedence, authoritative
Sales/Product/Inventory evidence ports, company authorization, idempotency, optimistic locking,
audit/outbox consistency, Flyway constraints, and PostgreSQL integration coverage.

Do not begin P3.12 or another bounded context until P3.11 is approved.

## P3.11 Boundaries

- No Inventory issue, replacement-stock execution, or balance mutation.
- No service invoicing or accounting posting.
- No technician scheduling or mobile workflow.
- No direct persistence access across bounded contexts.
