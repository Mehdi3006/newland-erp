# Control Domains

Control domains enforce operational discipline across transaction domains.

## Inventory Principles

Rules:

- Inventory is derived from posted movements.
- Direct balance editing is not allowed.
- On-hand stock is physically controlled quantity.
- Available stock equals on-hand stock minus unavailable quantities.
- Reserved stock is committed to demand.
- QC Hold stock is unavailable until released.
- Blocked stock is unavailable by policy.
- In Transit stock is moving but not physically available at destination.
- Damaged stock requires disposition.
- Consigned stock support is OPEN DECISION.
- Project stock must retain company and project ownership.
- Warehouse and location are required for physical stock custody.
- Unit conversions must be explicit.
- Carton and piece quantities must be consistent with packaging master data.
- Negative stock policy is OPEN DECISION.
- Lot, batch, and serial number readiness is required even if not immediately implemented.
- Stock count variances require adjustment approval.
- Transfers must have source, destination, status, and audit record.
- Returns must preserve source reference when available.
- Inventory valuation integration points must be defined before financial posting.
- Inventory ledger entries are immutable after posting.

## Workflow and Approval Principles

Rules:

- Workflow owns approval state, not source transaction data.
- Approval requests must reference a source context and source object.
- Approval decisions must be auditable.
- Amount limits and approval limits are capability rules, not hard-coded role names.
- Segregation of duties must prevent the same actor from preparing and approving sensitive actions
  where configured.

OPEN DECISION: Final workflow configuration model and escalation rules.

## Audit and Compliance Principles

Rules:

- Audit events must record actor, action, time, source context, source object, outcome, and scope.
- Sensitive field access must be audited.
- Posted accounting and inventory movements require immutable audit evidence.
- Export actions require audit.
- Emergency access requires reason, duration, approval, and review.

OPEN DECISION: Retention duration, legal hold process, and compliance export formats.

## Document Control Principles

Rules:

- Documents must be classified by source object, document type, sensitivity, and retention policy.
- Document deletion after use is not allowed unless retention and legal rules allow it.
- Versioning is required for documents that can be replaced.

OPEN DECISION: File storage provider, encryption policy, backup policy, and e-signature scope.
