# Implementation Roadmap

This roadmap is a sequencing proposal. It does not approve implementation until the relevant phase
is explicitly accepted.

## Phase P3: Foundation Implementation

| Phase | Scope                              | Goal                                                                                                   | Must resolve first                                     |
| ----- | ---------------------------------- | ------------------------------------------------------------------------------------------------------ | ------------------------------------------------------ |
| P3.1  | Enterprise Structure               | Implement enterprise, legal entity, company, branch, warehouse, zone, and location foundations.        | Approved and merged; project/scope expansion deferred. |
| P3.2  | Identity and Access                | Implement users, roles, permissions, scoped authorization, sessions, tokens, and password foundations. | Under P3.2 PR review.                                  |
| P3.3  | Product Information Management     | Implement product, SKU, brand, category, UOM, packaging, and lifecycle.                                | SKU/model/traceability policy.                         |
| P3.4  | Procurement                        | Implement supplier, RFQ, quotation, PO lifecycle.                                                      | Supplier model and approval limits.                    |
| P3.5  | Import Logistics                   | Implement shipment, container, customs milestones, landed-cost draft.                                  | Logistics/customs fields and cost basis.               |
| P3.6  | Inventory and Quality              | Implement receipt, QC hold/release, stock ledger, transfer, adjustment.                                | Negative stock, valuation, QC policy.                  |
| P3.7  | Sales                              | Implement quotation, sales order, reservation request, delivery/invoice handoff.                       | Pricing, credit, tax decisions.                        |
| P3.8  | CRM                                | Implement lead, opportunity, activity, customer timeline.                                              | Customer/business-partner decision.                    |
| P3.9  | Service and Warranty               | Implement service ticket and warranty validation foundation.                                           | Warranty policy and serial rules.                      |
| P3.10 | General Ledger and AR/AP           | Implement accounting foundation and source posting contracts.                                          | Fiscal, currency, COA, tax decisions.                  |
| P3.11 | Treasury, Banking, Checks, Cashbox | Implement payment, receipt, cashbox, check, bank reconciliation foundations.                           | Bank/payment/check policies.                           |
| P3.12 | Project Accounting                 | Implement project ledger and project stock/accounting references.                                      | Project ownership and budget policy.                   |

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
