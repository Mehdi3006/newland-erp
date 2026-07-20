# Transaction Domains

Transaction domains own business documents and lifecycle decisions. They may reference master data,
but they do not become master-data owners.

## Procurement and Supplier Management

Purpose: control supplier sourcing and purchase commitments.

Core documents:

- Demand request.
- RFQ.
- Supplier quotation.
- Comparison sheet.
- Proforma invoice.
- Purchase order.
- Supplier invoice handoff.

Rules:

- Approved purchase orders require supplier, company, currency, products, quantities, and terms.
- Purchase orders can be revised, not silently overwritten, after approval.
- Supplier quotation acceptance should publish an event.
- Production follow-up belongs to procurement until goods are ready for logistics handoff.
- Supplier invoice posting belongs to Accounts Payable.

OPEN DECISION: Final approval limits, supplier credit rules, and required proforma fields.

## Import and International Logistics

Purpose: track inbound movement from factory readiness to warehouse receipt.

Core documents:

- Shipment.
- Container.
- Booking.
- Packing list reference.
- Customs release reference.
- Landed-cost allocation draft.

Rules:

- Shipment must reference approved purchasing source documents.
- Container loading cannot precede shipment booking.
- Customs release is required before inland release where applicable.
- Landed cost belongs to logistics until posted by accounting.

OPEN DECISION: Country-specific customs documents, logistics provider interfaces, and landed-cost
allocation basis.

## Quality Management

Purpose: decide whether received or returned goods can become available stock.

Core documents:

- Inspection request.
- Inspection result.
- QC failure.
- Release decision.
- Block decision.

Rules:

- Goods under QC hold are not available.
- Failed inspection blocks stock until approved disposition.
- QC release publishes stock-release facts.
- QC must not directly edit stock balances.

OPEN DECISION: Inspection sampling rules, required QC attributes by product category, and
authorization for QC override.

## Sales and Distribution

Purpose: manage customer commercial commitments and fulfillment requests.

Core documents:

- Lead/opportunity handoff.
- Quotation.
- Sales order.
- Reservation request.
- Delivery instruction.
- Customer invoice handoff.
- Return request.

Rules:

- Approved sales order requires customer, company, product, quantity, price policy, and delivery
  scope.
- Credit check is required where credit sales are enabled.
- Reservation must be requested from Inventory; Sales does not change stock.
- Delivery posting creates accounting and inventory implications through published facts.

OPEN DECISION: Final pricing policy, discount limits, credit-control thresholds, and tax behavior.

## Warranty and Service

Purpose: manage after-sales cases and warranty obligations.

Core documents:

- Service ticket.
- Warranty validation.
- Diagnosis.
- Repair or replacement decision.
- Spare-part issue request.
- Customer response.
- Closure record.

Rules:

- Ticket closure requires documented resolution.
- Warranty validation must use approved sales/product evidence when available.
- Spare-part issue is an inventory movement.
- Replacement may create accounting and inventory impacts.

OPEN DECISION: Warranty period rules, serial-number requirements, and replacement accounting policy.
