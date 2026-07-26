# Next Codex Task

## Recommended Task: Review P3.9 Import Logistics

P3.9 is implemented on a feature branch and must pass architecture review before merge.

Review shipment and container lifecycle invariants, approved Purchase Order references, company
authorization, customs milestone auditability, landed-cost draft persistence, published events,
Flyway constraints, and PostgreSQL integration coverage.

Do not begin P3.9.5 or another bounded context until P3.9 is approved.

## P3.9 Boundaries

- No Inventory receipt or stock mutation.
- No landed-cost accounting posting or valuation.
- No carrier or customs provider integration.
- No Procurement persistence access; only its published reference API is used.
