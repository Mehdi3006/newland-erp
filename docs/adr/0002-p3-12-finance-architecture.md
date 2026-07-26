# ADR-0002: P3.12 General Ledger and AR/AP architecture

- Status: Proposed
- Date: 2026-07-26
- Owners: Newland ERP Architecture
- Deciders: Finance, Architecture, and Product Owners
- Consulted: Procurement, Sales, Inventory, Service, Tax, and Audit Owners
- Supersedes: None
- Superseded by: None

## Context

P3.7 established the Finance bounded context with company-scoped charts of accounts, fiscal years,
accounting periods, balanced journals, reversals, financial dimensions, and currency snapshots. P3.8
established Finance-owned accounting events, deterministic versioned posting rules, durable posting
requests, idempotency, retry, audit, and transactional outbox behavior. P3.9.1 connected approved
Procurement accounting facts to that published posting boundary.

P3.12 must add period-close controls, Accounts Payable, and Accounts Receivable without duplicating
the existing journal or posting engines. It must not move accounting rules into Procurement, Sales,
Inventory, or Service and Warranty. Treasury execution, bank reconciliation, cash management,
country-specific tax filing, inventory valuation, revenue recognition, and financial statements
remain outside P3.12.

Several business policies are not yet confirmed. This ADR therefore distinguishes architectural
decisions that can be accepted now from business decisions that must be supplied or explicitly
deferred before affected implementation slices begin.

## Decision drivers

- Preserve one immutable, double-entry accounting truth per company.
- Keep AP and AR operational subledgers isolated from GL journal persistence.
- Reuse the P3.8 posting engine as the only rule-driven journal creation path.
- Preserve source-document ownership in existing bounded contexts.
- Enforce company scope, period controls, idempotency, audit, and transactional outbox consistency.
- Support multiple countries and currencies without hard-coded statutory rules.
- Allow P3.12 to be delivered in independently reviewable slices.

## Considered options

1. Implement GL, AP, and AR inside the existing Finance package as one aggregate model.
2. Keep GL in Finance and introduce AP and AR as separate bounded contexts using published Finance
   posting contracts.
3. Let Procurement and Sales own supplier and customer invoices and create journals directly.

Option 2 is selected. Option 1 couples operational subledgers to GL persistence and makes ownership
and release boundaries unclear. Option 3 violates the established posting boundary and duplicates
accounting rules in operational modules.

## Decision

### 1. Bounded contexts and dependency direction

- General Ledger remains owned by the existing Finance bounded context.
- Accounts Payable and Accounts Receivable are separate bounded contexts and persistent subledgers.
- AP and AR may depend only on published reference or command ports from Enterprise Structure,
  Master Data, Procurement, Sales, Identity, Platform, and Finance.
- AP and AR never access Finance repositories or tables.
- Procurement, Sales, Inventory, and Service and Warranty never create `JournalEntry` directly.
- All rule-driven accounting uses the published P3.8 `FinancialPostingPort`, durable accounting
  events, posting rules, and transactional outbox.
- Treasury owns payment and receipt execution in P3.13. P3.12 records obligations, open items, and
  allocation contracts but does not execute money movement.

### 2. General Ledger design

#### Chart of accounts

- A chart of accounts is company-scoped. A company has one active primary operational chart at a
  time; alternate statutory or reporting mappings are separate mappings, not duplicate journal
  truths.
- Account codes are unique within a company and immutable after first posting.
- Accounts form an adjacency-list hierarchy with an optional parent account.
- Cycles, cross-company parents, and posting to non-postable parent accounts are prohibited.
- Account type is explicit: asset, liability, equity, revenue, expense, or control.
- Normal balance, posting eligibility, active dates, blocked state, and required dimensions are
  explicit account policy.
- Accounts with posted activity are retired or blocked; they are not deleted.

#### Journal entry model

- `JournalEntry` remains the aggregate root for a journal header and at least two journal lines.
- A journal is company-scoped and records journal number, source type and identifier, posting date,
  accounting period, description, transaction currency, base currency, rate snapshot, preparer,
  approver where required, posting actor, idempotency key, and optimistic-lock version.
- Each line records account, debit or credit amount, transaction amount, base amount, cost center,
  profit center, financial dimensions, tax reference where applicable, and source-line reference.
- Debit equals credit in company base currency. Zero and negative line values are prohibited.
- Drafts may be edited under optimistic locking. Posted journals and their snapshots are immutable.
- Correction uses a linked reversal or a new adjusting journal; deletion and in-place correction of
  posted data are prohibited.
- One accepted accounting event creates exactly one posting request and at most one journal.

#### Posting rules

- P3.8 posting rules remain the only configurable accounting-rule model.
- Rules are Finance-owned, versioned, effective-dated, deterministic, and immutable after use.
- Company-specific rules take precedence over global defaults.
- Overlapping rules at the same precedence and effective date are rejected.
- Account, amount, currency, tax, cost-center, profit-center, and financial-dimension resolution is
  performed by Finance adapters through authoritative ports.
- Manual journals use an explicit manual-journal application service and approval permission; they
  do not bypass journal invariants.

#### Period closing

- Accounting periods use `OPEN`, `CLOSING`, and `CLOSED` states.
- `OPEN` accepts authorized posting. `CLOSING` blocks ordinary source posting and permits only
  explicitly authorized close adjustments. `CLOSED` rejects all posting.
- Closing requires a persisted checklist, unresolved-posting check, subledger reconciliation
  results, close actor, approval actor, and close timestamp.
- Periods close in chronological order within a fiscal year unless a documented exception is
  approved.
- Reopening is a controlled, audited transition requiring a dedicated permission and reason. A
  reopened period returns to `CLOSING`, never directly to unrestricted `OPEN`.
- Closing and reopening publish durable domain events through the transactional outbox.

### 3. Accounts Payable

#### Supplier invoice lifecycle

- AP owns `SupplierAccount`, `SupplierInvoice`, `SupplierCreditMemo`, `PayableOpenItem`, and
  `PaymentAllocation`.
- The supplier master remains Procurement-owned and is referenced through a published supplier port.
- The lifecycle is `DRAFT` → `MATCHING` → `MATCHED` → `APPROVED` → `POSTED` → `PARTIALLY_SETTLED` →
  `SETTLED`. `REJECTED`, `DISPUTED`, `CANCELLED`, and `REVERSED` are controlled branches.
- A posted supplier invoice is immutable. Correction uses a supplier credit memo or reversal.
- Supplier invoice identity is unique by company, supplier, supplier invoice number, and invoice
  date, with a normalized duplicate-detection key.

#### Purchase invoice matching

- Matching uses published Procurement PO lines and Inventory receipt evidence; AP does not query
  their repositories.
- Three-way matching compares invoice, purchase order, and goods receipt quantities and amounts.
- Two-way matching is allowed only for explicitly configured non-receipted categories.
- Match results preserve source versions, quantities, prices, taxes, currency, variances, tolerance
  decisions, override actor, reason, and approval.
- An invoice cannot post while required matching exceptions remain unresolved.

#### Payment workflow and credit terms

- AP calculates due dates, discount dates, installments, and payable aging from a snapshot of Master
  Data payment terms captured at invoice approval.
- AP creates an approved payment request or exposes payable items to Treasury; it does not execute
  payments, store bank credentials, or mark cash movement successful.
- P3.13 Treasury returns immutable payment-execution facts. AP applies them idempotently to open
  items through a published allocation port.
- Allocation never exceeds the remaining payable amount. Partial settlement and unapplied payment
  amounts are preserved.

### 4. Accounts Receivable

#### Customer invoice lifecycle

- AR owns `CustomerAccount`, `CustomerInvoice`, `CustomerCreditMemo`, `ReceivableOpenItem`, and
  `ReceiptAllocation`.
- The customer master remains Sales-owned and is referenced through a published customer port.
- The lifecycle is `DRAFT` → `VALIDATED` → `APPROVED` → `POSTED` → `PARTIALLY_COLLECTED` →
  `COLLECTED`. `DISPUTED`, `CANCELLED`, `WRITTEN_OFF`, and `REVERSED` are controlled branches.
- Posted customer invoices are immutable. Correction uses a customer credit memo or reversal.
- Invoice creation requires an authorized source fact or an explicitly authorized direct-invoice
  flow. Delivery, service, and tax prerequisites are validated through published ports.

#### Aging and collection

- Receivable aging is derived from immutable open-item amounts, due dates, allocations, and an
  explicit as-of date; it is not stored as an independently editable balance.
- Aging buckets are configuration, scoped by company, and versioned.
- Collection workflow records follow-up status, promise-to-pay date, dispute status, owner, notes,
  and escalation. It does not send communications or execute receipts.
- Credit exposure is published to Sales through a read-only port. Sales remains responsible for
  order acceptance and customer status.

#### Payment allocation

- P3.13 Treasury owns receipt execution. AR consumes immutable receipt facts idempotently.
- Allocation supports partial, multi-invoice, and unapplied receipts while preserving transaction
  and base-currency amounts.
- Allocation cannot exceed either the receipt remainder or receivable remainder.
- Reallocation and reversal preserve the original allocation history and audit trail.

### 5. Financial foundation

#### Fiscal calendar

- Fiscal calendars are company-scoped and reference a legal entity.
- Fiscal years contain non-overlapping, contiguous accounting periods with explicit start and end
  dates.
- Posting date selects the period; callers cannot supply a conflicting period identifier.
- Calendar type and period count are configuration. The domain does not assume twelve Gregorian
  months.

#### Multi-currency and exchange rates

- Every financial document preserves transaction currency and amount.
- Every posted journal preserves company base currency and amount.
- Group reporting currency is optional and must not replace company base currency.
- Rates are resolved through the authoritative Master Data exchange-rate port by company, currency
  pair, rate type, and effective date.
- Posting stores the rate identifier, source, type, effective date, value, precision, and resolved
  amounts as immutable snapshots.
- Missing, expired, inverted without authorization, mismatched, or cross-company rates reject
  posting.
- Realized and unrealized FX accounting is deferred until its policy is approved.

#### Tax and statutory framework

- P3.12 provides country-neutral tax references and immutable tax-calculation snapshots only.
- Tax category, jurisdiction, registration, taxable basis, rate, tax amount, rounding, and source
  are explicit.
- Country-specific determination, filing, e-invoicing, withholding, and statutory report logic
  require separately approved country packs.
- No country tax rate or statutory account is hard-coded in shared domain code.

#### Numbering

- Existing Platform/Master Data number-series infrastructure is reused.
- Draft records use internal identifiers; legally relevant final numbers are allocated atomically at
  posting.
- Number series are scoped by document type, company, optional branch, fiscal year, and reset
  policy.
- Posted numbers are unique, auditable, never reused, and retained after reversal or cancellation.
- External supplier invoice numbers and legacy numbers remain separate references.

### 6. Integration contracts

#### Procurement

- Procurement remains owner of suppliers, purchase orders, and commercial commitments.
- AP consumes approved PO and supplier-invoice facts through published contracts.
- Existing P3.9.1 accounting publications enter Finance through P3.8 and must be reconciled with AP
  document identity before AP posting is enabled.
- Procurement never updates payable balances or journals.

#### Inventory

- Inventory owns receipts, issues, stock movements, quantities, lots, serials, and reservations.
- AP consumes receipt evidence for matching.
- GL consumes approved valuation accounting events only after inventory valuation and landed-cost
  policies are approved.
- P3.12 does not calculate inventory cost or mutate inventory.

#### Sales

- Sales owns customers, quotations, orders, reservation requests, and delivery tracking.
- AR consumes authorized delivery or invoice-request facts through published contracts.
- AR publishes credit exposure through a read-only contract.
- Sales never updates receivable balances or journals.

#### Service and Warranty

- Service and Warranty owns ticket, warranty, diagnosis, and resolution facts.
- No automatic invoice or journal is created from a service ticket in P3.12.
- A future billable-service event may request an AR invoice only after pricing, tax, warranty-cost,
  replacement-cost, and authorization policies are approved.
- Warranty cost or replacement write-off posting remains deferred.

### 7. Cross-cutting controls

- Authentication, capability authorization, and company scope are mandatory on every command and
  query.
- Approval capabilities are distinct from preparation and posting capabilities.
- Idempotency keys and database uniqueness prevent duplicate invoices, allocations, posting
  requests, and journals.
- Optimistic or database locking protects approval, posting, allocation, close, and reopen
  transitions.
- Document, audit, outbox, and accounting-event changes commit atomically.
- REST APIs expose application DTOs and RFC 9457 Problem Details, never persistence models.
- PostgreSQL constraints, Flyway migrations, jOOQ adapters, Testcontainers, ArchUnit, and Spring
  Modulith tests are mandatory for each implementation slice.

## Open decisions requiring business input

The following items block only the affected implementation slice unless explicitly stated.

| Decision                   | Required business input                                                                                               | Blocks                                  |
| -------------------------- | --------------------------------------------------------------------------------------------------------------------- | --------------------------------------- |
| Legal ledgers and charts   | Primary chart per company, statutory chart requirements, consolidation mappings, retained-earnings accounts           | GL account setup and year close         |
| Fiscal calendars           | Calendar type, number of periods, adjustment periods, year boundaries, close timetable, reopening authority           | GL period close                         |
| Journal approvals          | Amount thresholds, segregation-of-duties matrix, close-adjustment authority, reversal authority                       | Manual journals and close               |
| Currency policy            | Base currency per company, group currency, approved rate sources/types, triangulation, stale-rate tolerance, rounding | Foreign-currency posting                |
| FX accounting              | Realized/unrealized recognition, revaluation frequency, gain/loss accounts                                            | Settlement and revaluation              |
| Tax jurisdictions          | Registrations, invoice tax point, inclusive/exclusive pricing, rounding, exemptions, withholding, reverse charge      | Tax-bearing AP/AR posting               |
| Statutory invoicing        | Country numbering gaps, fiscal-authority/e-invoice requirements, cancellation and credit-note rules, retention period | Final customer/supplier invoice posting |
| AP matching                | Two-way/three-way applicability, quantity/price/tax tolerances, service and landed-cost treatment, override roles     | AP matching and posting                 |
| AP payment terms           | Due-date conventions, holidays, installments, early-payment discounts, late charges                                   | AP due dates and aging                  |
| AR credit policy           | Credit-limit source, exposure calculation, hold/override rules, disputed and overdue treatment                        | AR exposure and Sales credit checks     |
| AR collection policy       | Aging buckets, escalation stages, promise-to-pay rules, write-off thresholds and authority                            | Aging and collection                    |
| Receipt/payment allocation | Automatic allocation order, over/underpayment, prepayments, on-account balances, cross-currency allocation            | AP/AR settlement contracts              |
| Inventory valuation        | Cost method, valuation point, landed-cost allocation, variance accounts                                               | Inventory-to-GL posting                 |
| Service accounting         | Billable service trigger, warranty expense, replacement cost, write-off and recovery policy                           | Service-to-AR/GL integration            |

## Recommended P3.12 implementation sequence

1. **P3.12.0 — Decision closure and contracts:** approve or explicitly defer the blocking business
   decisions; define published AP, AR, GL-close, Treasury-allocation, and source-evidence contracts.
2. **P3.12.1 — General Ledger controls:** extend the existing Finance model with account policy,
   manual-journal approval, period close/reopen, close checklist, reconciliation evidence, and
   permissions. Do not recreate journals or posting rules.
3. **P3.12.2 — Accounts Payable core:** implement supplier accounts, invoice/credit lifecycle,
   duplicate protection, terms snapshots, open items, and PostgreSQL persistence.
4. **P3.12.3 — AP matching and posting:** add PO/receipt matching, tolerance decisions, explicit
   posting events, Finance posting integration, audit, outbox, retry, and reconciliation.
5. **P3.12.4 — Accounts Receivable core:** implement customer accounts, invoice/credit lifecycle,
   terms snapshots, open items, aging, collection state, and PostgreSQL persistence.
6. **P3.12.5 — AR posting and allocation contracts:** add authorized source facts, Finance posting
   integration, Treasury receipt-allocation contracts, audit, outbox, retry, and reconciliation.
7. **P3.12.6 — Cross-subledger hardening:** verify GL-to-subledger reconciliation, concurrency,
   idempotency, rollback, company isolation, period close behavior, and migration integrity across
   real PostgreSQL integrations.

Each step requires an independent engineering and architecture gate. P3.13 payment, receipt, bank,
check, and cash execution must not begin as part of P3.12.

## Consequences

### Positive

- GL remains the single accounting truth while AP and AR retain clear operational ownership.
- Existing P3.7 and P3.8 investments are reused instead of duplicated.
- Country-specific requirements can be introduced through reviewed policies and country packs.
- AP and AR can be reviewed and released independently.

### Negative

- More published contracts and reconciliation tests are required.
- P3.12 cannot be completed as one undifferentiated implementation increment.
- Several business decisions must be supplied before tax, settlement, and close behavior is final.

### Neutral or follow-up

- P3.13 will implement Treasury execution and feed immutable payment/receipt facts back to AP/AR.
- Inventory valuation, service accounting, statutory packs, and financial statements remain
  separately gated.
- This ADR remains `Proposed` until Finance, Product, Architecture, and affected business owners
  approve the decisions or record explicit deferrals.

## Validation

- Spring Modulith and ArchUnit tests prove GL, AP, AR, and source-context boundaries.
- PostgreSQL tests prove account hierarchy, invoice uniqueness, period controls, open-item
  invariants, allocations, optimistic locking, and company isolation.
- Transaction tests prove document, accounting event, audit, and outbox atomicity and rollback.
- Concurrency tests prove at-most-once posting and allocation.
- Contract tests prove source modules use only published ports and never Finance persistence.
- Architecture review must reject implementation that hard-codes unresolved country, tax, FX,
  matching, credit, or numbering policy.

## References

- [Financial Domains](../business-architecture/07-financial-domains/README.md)
- [Context Map](../business-architecture/03-context-map/README.md)
- [Process Map](../business-architecture/10-process-map/README.md)
- [Domain Event Catalog](../business-architecture/11-event-catalog/README.md)
- [Permission Model](../business-architecture/12-permission-model/README.md)
- [Numbering and Status Architecture](../business-architecture/13-numbering-and-status/README.md)
- [Open Architecture Decisions](../business-architecture/17-decisions/README.md)
- [P3.8 Financial Posting Infrastructure](../architecture/finance-posting-infrastructure.md)
- [Implementation Roadmap](../control/IMPLEMENTATION_ROADMAP.md)
