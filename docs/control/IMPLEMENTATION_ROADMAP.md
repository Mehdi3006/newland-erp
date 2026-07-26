# Implementation Roadmap

This roadmap is a sequencing proposal. It does not approve implementation until the relevant phase
is explicitly accepted.

## Phase P3: Foundation Implementation

| Phase  | Scope                              | Goal                                                                                                                                                                                                  | Must resolve first                                       |
| ------ | ---------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------- |
| P3.1   | Enterprise Structure               | Implement enterprise, legal entity, company, branch, warehouse, zone, and location foundations.                                                                                                       | Approved and merged; project/scope expansion deferred.   |
| P3.2   | Identity and Access                | Implement users, roles, permissions, scoped authorization, sessions, tokens, and password foundations.                                                                                                | Approved and merged.                                     |
| P3.2.5 | Platform Foundation                | Implement shared infrastructure: event bus, outbox, audit, jobs, storage, configuration, flags, cache, localization, and catalogs.                                                                    | Approved and merged.                                     |
| P3.3   | Master Data                        | Implement enterprise reference-data foundation for organization, geography, currency, UOM, tax, payment, shipping, fiscal, numbering, document, attachment, and product-classification references.    | Approved and merged.                                     |
| P3.3.5 | Shared Product Catalog             | Implement product, SKU, code, barcode, category/brand/family assignment, attributes, UOM assignment, packaging, measurements, media, localized content, tags, search metadata, and warranty metadata. | Approved and merged.                                     |
| P3.3.x | Product Information Management     | Implement deeper SKU traceability and lifecycle extensions after Shared Product Catalog is approved.                                                                                                  | Traceability policy.                                     |
| P3.4   | Inventory Foundation               | Implement stock transactions, ledger, balances, reservations, lots, serials, statuses, reversals, idempotency, and inventory audit foundations.                                                       | Approved and merged.                                     |
| P3.5   | Procurement Foundation             | Implement supplier, supplier product reference, requisition, approval, RFQ, supplier quotation, comparison, purchase order, amendment, cancellation, and partial-delivery foundations.                | Approved and merged.                                     |
| P3.6   | Sales Foundation                   | Implement customer, quotation, sales order, reservation request, delivery request, amendment, cancellation, and partial-fulfilment foundations.                                                       | Approved and merged; no AR/accounting/pricing.           |
| P3.7   | Finance Foundation                 | Implement chart of accounts, fiscal periods, journal drafting/posting/reversal, financial dimensions, and explicit future finance posting contracts.                                                  | Approved and merged; no AP/AR/auto-posting.              |
| P3.8   | Financial Posting Infrastructure   | Implement immutable accounting events, deterministic posting rules, posting requests, retries, audit, outbox, and explicit Finance journal delegation.                                                | Approved and merged; no automatic source-module posting. |
| P3.9.1 | Procurement to Finance Integration | Publish five approved Procurement accounting facts through the Finance Posting Engine published API without accounting rules or direct journal creation in Procurement.                               | Approved and merged.                                     |
| P3.9   | Import Logistics                   | Implement shipment, container, customs milestones, landed-cost draft.                                                                                                                                 | Approved and merged.                                     |
| P3.9.5 | Inventory and Quality Extensions   | Implement QC hold/release and advanced inventory operations after Inventory Foundation approval.                                                                                                      | Valuation and QC policy.                                 |
| P3.10  | CRM                                | Implement lead, opportunity, activity, customer timeline.                                                                                                                                             | Approved and merged.                                     |
| P3.11  | Service and Warranty               | Implement service ticket and warranty validation foundation.                                                                                                                                          | Implemented; awaiting architecture review.               |
| P3.12  | General Ledger and AR/AP           | Implement accounting foundation and source posting contracts.                                                                                                                                         | Fiscal, currency, COA, tax decisions.                    |
| P3.13  | Treasury, Banking, Checks, Cashbox | Implement payment, receipt, cashbox, check, bank reconciliation foundations.                                                                                                                          | Bank/payment/check policies.                             |
| P3.14  | Project Accounting                 | Implement project ledger and project stock/accounting references.                                                                                                                                     | Project ownership and budget policy.                     |

## Phase P4: Integrations and Advanced Operations

- Website synchronization.
- Barcode/mobile readiness.
- Payment and banking integrations.
- Logistics and customs integrations.
- Reporting semantic layer.
- AI assistant with strict approval and audit boundaries.

## Roadmap Rules

- Do not implement a module before its open decisions are resolved or explicitly deferred.
- Do not add UI before backend domain and permission contracts are accepted.
- Do not integrate external systems before ownership, idempotency, and audit rules are approved.
