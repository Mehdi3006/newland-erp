# Next Codex Task

## Recommended Task: Review P3.10 CRM

P3.10 is implemented on a feature branch and must pass architecture review before merge.

Review lead and opportunity lifecycle invariants, Sales-owned customer references, company and
branch authorization, activity immutability, customer timeline isolation, idempotency, audit and
outbox consistency, Flyway constraints, and PostgreSQL integration coverage.

Do not begin P3.11 or another bounded context until P3.10 is approved.

## P3.10 Boundaries

- No CRM campaign or marketing-automation implementation.
- No Sales quotation, order, pricing, invoicing, or delivery behavior.
- No duplicate customer master; CRM consumes only the Sales published customer reference.
- No Service and Warranty behavior.
