# Module Registry

This registry tracks business architecture readiness. It is not a code module registry.

| Module / context                | P2 business architecture | Implementation status | Notes                                                                  |
| ------------------------------- | ------------------------ | --------------------- | ---------------------------------------------------------------------- |
| Enterprise Structure            | Defined                  | Implemented           | P3.1 approved and merged.                                              |
| Identity and Access             | Defined                  | Implemented           | P3.2 approved and merged.                                              |
| Master Data                     | Defined                  | Implemented           | P3.3 approved and merged.                                              |
| Product Information Management  | Defined                  | Implemented           | P3.3.5 approved and merged; no inventory/pricing.                      |
| Inventory                       | Defined                  | Implemented           | P3.4 approved and merged.                                              |
| Procurement                     | Defined                  | Implemented           | P3.5 approved and merged; no AP/accounting/stock mutation.             |
| Import Logistics                | Defined                  | Implemented           | P3.9 approved and merged; external integrations remain deferred.       |
| Quality Management              | Defined                  | Not started           | Requires inspection-policy decisions.                                  |
| Sales                           | Defined                  | Implemented           | P3.6 approved and merged; no AR/accounting/pricing/stock mutation.     |
| Finance                         | Defined                  | Implemented           | P3.7 approved and merged; no AP/AR/automatic subledger posting.        |
| Finance Posting Infrastructure  | Defined                  | Implemented           | P3.8 approved and merged; no Procurement/Sales/Inventory auto-posting. |
| Procurement-Finance Integration | Defined                  | Implemented           | P3.9.1 approved and merged.                                            |
| CRM                             | Defined                  | Implemented           | P3.10 approved and merged; Sales remains customer master owner.        |
| Service and Warranty            | Defined                  | Implemented           | P3.11 approved and merged.                                             |
| Finance Foundation Contracts    | Defined                  | In review             | P3.12.0 contracts only; no AP/AR workflows.                            |
| General Ledger                  | Defined                  | Not started           | P3.12.1 requires fiscal, tax, currency, and COA decisions.             |
| Accounts Receivable             | Defined                  | Not started           | Requires credit policy.                                                |
| Accounts Payable                | Defined                  | Not started           | Requires invoice matching policy.                                      |
| Treasury                        | Defined                  | Not started           | Requires bank/payment/check decisions.                                 |
| Project Accounting              | Defined                  | Not started           | Requires project ownership and budget decisions.                       |
| Fixed Assets                    | Defined                  | Not started           | Requires depreciation/capitalization decisions.                        |
| Human Resources                 | Defined                  | Not started           | Requires HR sensitive-field policy.                                    |
| Payroll                         | Defined                  | Not started           | Country-specific payroll rules are open.                               |
| Document Management             | Defined                  | Not started           | Storage provider decision pending.                                     |
| Workflow and Approval           | Defined                  | Not started           | Approval engine configuration model pending.                           |
| Reporting                       | Defined                  | Not started           | BI semantic layer pending.                                             |
| Notification                    | Defined                  | Not started           | Channel and consent policy pending.                                    |
| Integration                     | Defined                  | Not started           | Priority and auth standards pending.                                   |
| System Administration           | Defined                  | Not started           | Implementation deferred.                                               |

## Platform Foundation

| Foundation area | Implementation status | Notes                                            |
| --------------- | --------------------- | ------------------------------------------------ |
| Shared Platform | Implemented           | P3.2.5 approved and merged; no business modules. |
