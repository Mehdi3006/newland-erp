# Integration Map

P2 documents future integrations only. No integration is implemented.

| Integration               | Direction                                         | Data exchanged                                                  | Owning context                                | Authentication class                         | Frequency                                 | Failure handling                          | Audit requirements                                        | Data sensitivity                         |
| ------------------------- | ------------------------------------------------- | --------------------------------------------------------------- | --------------------------------------------- | -------------------------------------------- | ----------------------------------------- | ----------------------------------------- | --------------------------------------------------------- | ---------------------------------------- |
| Newland website           | ERP -> Website and Website -> ERP OPEN DECISION   | Product catalog, availability, customer inquiry, order request  | Integration with Product, Sales, CRM          | API credential or OAuth OPEN DECISION        | Near real time or scheduled OPEN DECISION | Retry, dead-letter, manual reconciliation | Sync request, payload summary, actor/system               | Product public; customer/order sensitive |
| WhatsApp Business         | ERP -> External                                   | Notifications, service updates, approval alerts OPEN DECISION   | Notification / Integration                    | Channel token OPEN DECISION                  | Event driven                              | Retry with delivery status                | Message template, recipient, delivery result              | Customer contact sensitive               |
| Email                     | ERP -> External and External -> ERP OPEN DECISION | Notifications, document delivery, inbound parsing OPEN DECISION | Notification / Document Management            | SMTP/API credential OPEN DECISION            | Event driven                              | Retry and bounce tracking                 | Recipient, subject classification, delivery status        | Sensitive depending content              |
| Excel import/export       | External -> ERP and ERP -> External               | Master data, reports, migration files                           | Integration / Reporting / owning contexts     | User session permission                      | On demand                                 | Validation errors with row-level report   | User, file name, rows accepted/rejected                   | Often sensitive                          |
| PDF generation            | ERP -> Document                                   | Documents, reports, statements                                  | Document Management / Reporting               | Internal service credential OPEN DECISION    | On demand                                 | Render failure retry/log                  | Template, source object, generator                        | Sensitive depending document             |
| Barcode                   | Device -> ERP and ERP -> device                   | Product, location, serial, movement scan                        | Inventory / Product                           | Device/user auth OPEN DECISION               | Real time                                 | Offline queue OPEN DECISION               | Scan actor/device/time                                    | Operational sensitive                    |
| Payment gateways          | ERP <-> External                                  | Payment request, status, settlement                             | Treasury / Integration                        | Provider credential OPEN DECISION            | Event driven                              | Idempotent retry and reconciliation       | Payment ID, provider ref, status                          | Highly financial                         |
| Banking interfaces        | ERP <-> Bank                                      | Statements, payment files, reconciliation data                  | Banking / Treasury                            | Bank certificate/API OPEN DECISION           | Scheduled or event driven                 | Retry, bank reject handling               | File/API ref, status, approver                            | Highly financial                         |
| Logistics carriers        | ERP <-> Carrier                                   | Booking, tracking, shipment status                              | Import Logistics / Integration                | Carrier API credential OPEN DECISION         | Scheduled/event                           | Retry and manual update fallback          | Shipment ref, carrier response                            | Commercial sensitive                     |
| Customs data              | External -> ERP and ERP -> External OPEN DECISION | Customs release, duties, documents                              | Import Logistics / Finance                    | Government/provider credential OPEN DECISION | Per shipment                              | Manual exception queue                    | Customs ref, status, document evidence                    | Compliance sensitive                     |
| Google Drive backup       | ERP -> External                                   | Backup artifacts or exported documents OPEN DECISION            | System Administration / Document Management   | Service account OPEN DECISION                | Scheduled OPEN DECISION                   | Alert and retry                           | Backup job, path, result                                  | Highly sensitive                         |
| Future AI assistant       | ERP -> AI and AI -> ERP OPEN DECISION             | Retrieval context, suggestions, summaries                       | Integration / Reporting / owning contexts     | Service credential and user authorization    | On demand                                 | No autonomous posting without approval    | Prompt metadata, source docs, user, output classification | Highly sensitive                         |
| Future mobile application | Mobile <-> ERP                                    | Warehouse scans, service tickets, approvals OPEN DECISION       | Integration with Inventory, Service, Workflow | User/device auth                             | Real time/offline OPEN DECISION           | Offline sync conflict policy              | User/device/action/status                                 | Sensitive                                |

## Integration Principles

- Every integration has one owning context.
- External systems are never source of truth unless explicitly approved.
- External records require mapping and idempotency keys.
- Failed integrations must be visible to operators.
- Sensitive data exposure must be minimized.
- Authentication and transport technology are OPEN DECISION until approved by ADR.

## OPEN DECISION

- Integration priority order.
- Authentication standards.
- Whether Google Drive is backup, document archive, or both.
- AI assistant data-access boundaries.
- Mobile offline conflict resolution.
