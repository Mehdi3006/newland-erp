# Next Codex Task

## Recommended Task: Review P3.12.0 Finance Foundation Contracts

P3.12.0 is implemented on a feature branch and must pass engineering and architecture review before
merge.

Review accounting-period state and posting-purpose validation, balanced journal reconciliation
contracts, immutable financial-document numbering assignments, company-scoped currency and
exchange-rate snapshots, published application ports, posting-message validation, DTO isolation, and
Spring Modulith boundaries.

Do not begin P3.12.1, AP, AR, Treasury, or another bounded context until P3.12.0 is approved.

## P3.12.0 Boundaries

- No AP supplier-invoice or payment workflow.
- No AR customer-invoice, collection, aging, or allocation workflow.
- No new Finance persistence or migration.
- No UI or new REST endpoint.
- Existing P3.7 Finance and P3.8 Posting Engine behavior remains authoritative.
- Existing Procurement, Inventory, Sales, and Service/Warranty integrations remain unchanged.
