# Cross-Cutting Domains

## Product Principles

Rules:

- SKU and model are different concepts.
- Product identity is owned by Product Information Management.
- Brand, group, category, and subcategory classify products.
- Technical attributes must be structured enough for search, import, website, and service.
- Packaging must capture carton quantity, net weight, gross weight, dimensions, and CBM when used.
- Country of origin and HS Code are required readiness fields for import, but final validation is
  OPEN DECISION.
- Warranty attributes are referenced by Service and Warranty.
- Images, manuals, brochures, and certificates are document-managed assets.
- Product lifecycle must support draft, active, blocked, discontinued, and archived states.
- Duplicate detection is required before product activation.
- Product names and descriptions must support localization.
- Website synchronization readiness is required; website implementation is not approved in P2.

Small examples such as `NL-OPEN-DECISION-MODEL` may be used in future validation workshops, but P2
does not create a fake catalog.

## Localization

Responsibilities:

- Language readiness for Persian RTL and English LTR.
- Multi-currency readiness.
- Country, port, incoterm, tax-code placeholder ownership.
- Date, number, and monetary formatting rules.

OPEN DECISION: Specific statutory, tax, invoice, and reporting rules for UAE, Iran, Iraq, China, and
GCC countries.

## Reporting and BI

Rules:

- Reporting is read-only.
- Reports must define data owner, filters, dimensions, measures, refresh, access, and export rules.
- BI metrics must be traceable to source contexts.
- Dashboards are not implemented in P2.

## Integration Management

Rules:

- External integrations use anticorruption layers.
- External writes require idempotency.
- Failed integration attempts require retry and audit policy.
- Transport technology is not selected in P2.

## Notifications

Rules:

- Notification content must not leak sensitive fields.
- Templates require owner, channel, language, and approval.
- Delivery failure is auditable.

OPEN DECISION: Approved channels, consent policy, and customer-facing notification language.
