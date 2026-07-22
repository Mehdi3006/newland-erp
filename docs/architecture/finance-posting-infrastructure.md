# P3.8 Financial Posting Infrastructure

P3.8 provides Finance-owned generic posting infrastructure only. Immutable `AccountingEvent`
payloads enter through `FinancialPostingPort`, deterministic versioned rules are resolved, and
journal creation is delegated through an explicit Finance port.

No Procurement, Sales, Inventory, payment, banking, tax, fixed-asset, payroll, budgeting,
consolidation, statement, or reporting workflow is connected. Source contexts remain descriptive
event producers and are never queried by Finance.
