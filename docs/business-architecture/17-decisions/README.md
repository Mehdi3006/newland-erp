# Business Architecture Decisions and Open Decisions

This file centralizes unresolved business decisions discovered during P2.

## Confirmed Decisions

- P2 is documentation-first and does not implement runtime code.
- Newland ERP uses a modular business architecture with bounded contexts.
- Direct stock balance editing is not allowed.
- Posted accounting journals are immutable.
- Reversal is preferred over deletion after posting.
- Permissions are capability-based with scope dimensions.
- Reporting is read-only and does not own source data.
- External integrations require anticorruption layers and idempotency.

## OPEN DECISION

| Area                    | Decision required                                                                                    | Why it matters                                                        |
| ----------------------- | ---------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------- |
| Legal structure         | Confirm exact legal entities, companies, branches, and warehouses.                                   | Determines scope, numbering, ledgers, and reporting.                  |
| Tax and statutory       | Confirm UAE, Iran, Iraq, China, and GCC tax/statutory rules.                                         | Required before invoicing, posting, and statutory reports.            |
| Fiscal calendars        | Confirm fiscal year and period rules per legal entity.                                               | Required for accounting period close and numbering.                   |
| Currency                | Confirm base currency per legal entity and group reporting currency.                                 | Required for GL, AR/AP, treasury, and reporting.                      |
| Customer/Supplier model | Decide whether to introduce a Business Partner context.                                              | Affects master-data ownership and duplicate detection.                |
| Project ownership       | Decide whether Project master belongs to Enterprise Structure or Project Accounting.                 | Affects project warehouse and project ledger design.                  |
| Inventory valuation     | Select valuation method and landed-cost posting policy.                                              | Required before inventory/accounting implementation.                  |
| Negative stock          | Decide whether negative stock is ever allowed.                                                       | Affects reservation, delivery, and stock count behavior.              |
| Lot/batch/serial        | Decide product categories requiring traceability.                                                    | Affects warehouse, warranty, and service design.                      |
| Approval limits         | Define amount thresholds by company, role, branch, and project.                                      | Required for procurement, sales, payments, adjustments, and journals. |
| Numbering gap policy    | Define legal and operational gap handling by document type/country.                                  | Required before invoice and posted-document implementation.           |
| Warranty policy         | Define warranty period, eligibility, serial evidence, and replacement rules.                         | Required before service implementation.                               |
| Payment and checks      | Define country-specific check and banking lifecycle rules.                                           | Required for treasury implementation.                                 |
| Integration priority    | Rank website, WhatsApp, email, Excel, PDF, barcode, payments, banks, logistics, customs, AI, mobile. | Determines P4+ integration roadmap.                                   |
| Authentication          | Decide identity provider and authentication class.                                                   | Required before user management implementation.                       |
| Data retention          | Define audit, document, financial, HR, and integration retention policies.                           | Required for compliance and storage architecture.                     |
| AI assistant            | Define allowed data access, human approval, and audit rules.                                         | Prevents unsafe automation.                                           |

## Decision Process

- Resolve high-risk decisions before implementing affected modules.
- Record material architecture decisions as ADRs.
- Keep country-specific legal/tax items open until validated by qualified advisors.
