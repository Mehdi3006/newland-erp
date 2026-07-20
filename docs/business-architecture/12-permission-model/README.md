# Permission Model

Permissions are capability-based and use the format `context.resource.action`.

## Permission Catalog

| Permission                         | Description                              | Scope dimensions                                | Sensitive?       |
| ---------------------------------- | ---------------------------------------- | ----------------------------------------------- | ---------------- |
| organization.company.read          | Read company and legal-entity structure. | Enterprise, Legal Entity, Company               | No               |
| organization.company.manage        | Create or update company structure.      | Enterprise, Legal Entity, Company               | Yes              |
| organization.branch.manage         | Activate, suspend, or update branches.   | Company, Branch                                 | Yes              |
| organization.warehouse.manage      | Activate, suspend, or update warehouses. | Company, Branch, Warehouse                      | Yes              |
| identity.user.manage               | Create users and assign access.          | Enterprise, Company                             | Yes              |
| identity.role.assign               | Assign roles and scoped permissions.     | Enterprise, Company, Branch, Warehouse, Project | Yes              |
| product.product.create             | Create product master records.           | Enterprise, Company OPEN DECISION               | No               |
| product.product.update             | Update product details and packaging.    | Enterprise, Company OPEN DECISION               | Yes              |
| inventory.receipt.post             | Post goods receipt.                      | Company, Branch, Warehouse                      | Yes              |
| inventory.reservation.create       | Reserve stock for demand.                | Company, Warehouse, Project                     | Yes              |
| inventory.transfer.post            | Post stock transfer.                     | Company, Warehouse, Project                     | Yes              |
| inventory.adjustment.approve       | Approve stock adjustment.                | Company, Warehouse                              | Yes              |
| procurement.purchase-order.create  | Create purchase order draft.             | Company, Branch, Project                        | Yes              |
| procurement.purchase-order.approve | Approve purchase order.                  | Company, Branch, Project, Amount limit          | Yes              |
| sales.quotation.create             | Create customer quotation.               | Company, Branch, Sales Region                   | No               |
| sales.sales-order.approve          | Approve sales order.                     | Company, Branch, Amount limit                   | Yes              |
| accounting.journal.post            | Post journal entry.                      | Legal Entity, Company, Period                   | Yes              |
| accounting.period.close            | Close accounting period.                 | Legal Entity, Company                           | Yes              |
| receivable.invoice.post            | Post customer invoice.                   | Company, Branch                                 | Yes              |
| payable.invoice.post               | Post supplier invoice.                   | Company, Branch                                 | Yes              |
| treasury.payment.approve           | Approve payment execution.               | Company, Bank Account, Cashbox, Amount limit    | Yes              |
| treasury.receipt.post              | Post receipt or collection.              | Company, Branch, Cashbox, Bank Account          | Yes              |
| service.ticket.close               | Close service ticket.                    | Company, Branch, Service Center                 | Yes              |
| workflow.approval.decide           | Decide assigned approval step.           | Source scope, Amount limit                      | Yes              |
| reporting.report.read              | Run approved report.                     | Enterprise, Company, Branch, Warehouse, Project | Varies           |
| audit.log.read                     | Read audit log.                          | Enterprise, Company                             | Highly sensitive |
| integration.job.manage             | Manage integration jobs.                 | Integration endpoint scope                      | Yes              |
| administration.setting.manage      | Change system settings.                  | Enterprise, Company                             | Highly sensitive |

## Role Matrix

| Role                 | Initial permission profile                                                                                                                       |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------ |
| System Administrator | organization.company.read, identity.user.manage, identity.role.assign, administration.setting.manage, audit.log.read                             |
| CEO                  | organization.company.read, reporting.report.read, audit.log.read                                                                                 |
| Finance Manager      | accounting.journal.post, accounting.period.close, treasury.payment.approve, receivable.invoice.post, payable.invoice.post, reporting.report.read |
| Accountant           | accounting.journal.post, receivable.invoice.post, payable.invoice.post, treasury.receipt.post, reporting.report.read                             |
| Procurement Manager  | procurement.purchase-order.create, procurement.purchase-order.approve, reporting.report.read                                                     |
| Procurement User     | procurement.purchase-order.create, reporting.report.read                                                                                         |
| Warehouse Manager    | inventory.receipt.post, inventory.transfer.post, inventory.adjustment.approve, reporting.report.read                                             |
| Warehouse Operator   | inventory.receipt.post, inventory.reservation.create, inventory.transfer.post                                                                    |
| QC Inspector         | inventory.receipt.post, reporting.report.read                                                                                                    |
| Sales Manager        | sales.quotation.create, sales.sales-order.approve, reporting.report.read                                                                         |
| Sales User           | sales.quotation.create, reporting.report.read                                                                                                    |
| CRM User             | sales.quotation.create, reporting.report.read                                                                                                    |
| Service Manager      | service.ticket.close, inventory.transfer.post, reporting.report.read                                                                             |
| Service User         | service.ticket.close, inventory.reservation.create                                                                                               |
| Auditor              | audit.log.read, reporting.report.read                                                                                                            |
| HR Manager           | reporting.report.read, identity.role.assign OPEN DECISION                                                                                        |
| Payroll User         | reporting.report.read, treasury.payment.approve OPEN DECISION                                                                                    |
| Project Manager      | reporting.report.read, inventory.reservation.create, procurement.purchase-order.create                                                           |

## Scope Rules

- Company scope restricts legal and operating entity data.
- Branch scope restricts branch operational records.
- Warehouse scope restricts stock custody operations.
- Project scope restricts project ledger, project stock, and project documents.
- Own-record restrictions apply to assigned tasks, sales ownership, and service tickets where
  configured.
- Amount limits apply to purchase approval, sales approval, payment approval, adjustment approval,
  and write-off approval.
- Approval limits are distinct from role names.
- Segregation of duties can prevent creator/approver, invoice/payment approver, and journal
  preparer/poster combinations.
- Sensitive-field access requires separate permission even when the record is readable.
- Export permissions are separate from read permissions.
- Override permissions must require reason and audit.
- Emergency access must be time-bound, approved, logged, reviewed, and revoked.

## OPEN DECISION

- Final role names and organization-specific assignments.
- Approval amount thresholds by company, branch, project, and role.
- Sensitive-field list by country and context.
- Identity provider and authentication method.
