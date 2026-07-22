# Changelog

All notable changes to Newland ERP are documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and releases follow
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Phase P3.7 Finance Foundation for chart of accounts, account hierarchy, fiscal years, accounting
  periods, double-entry journal drafting/posting/reversal, cost and profit centers, currency and
  exchange-rate snapshots, audit, outbox, and explicit future finance posting ports.

- Phase P3.6 Sales Foundation for customers, contacts, addresses, customer credit profiles, customer
  product references, sales quotations, approvals, revisions, expiry enforcement, sales orders,
  approvals, amendments, cancellation, reservation and delivery request tracking, idempotency
  protection, attachment/audit/number-series reuse, and explicit inventory
  availability/reservation/delivery requests through ports.
- Versioned Sales REST APIs under `/api/v1/sales`, RFC 9457 Problem Details handling, OpenAPI
  grouping, Flyway migration, jOOQ repository, Spring Modulith and ArchUnit checks, and PostgreSQL
  Testcontainers coverage for P3.6.
- Phase P3.5 Procurement Foundation for suppliers, contacts, addresses, supplier product references,
  purchase requisitions, approvals, RFQs, supplier invitations, supplier quotations, auditable
  quotation comparison, purchase orders, approvals, amendments, cancellation, partial delivery
  tracking, idempotency protection, attachment/audit/number-series reuse, and explicit inventory
  receipt requests through a port.
- Versioned Procurement REST APIs under `/api/v1/procurement`, RFC 9457 Problem Details handling,
  OpenAPI grouping, Flyway migration, jOOQ repository, Spring Modulith and ArchUnit checks, and
  PostgreSQL Testcontainers coverage for P3.5.
- Phase P3.4 Inventory Foundation for stock transactions, movement lines, append-only stock ledger,
  derived balances, reservations/releases, lots, serial numbers, inventory statuses, expiry checks,
  reversals, idempotency protection, transaction immutability, audit/event/outbox/attachment reuse,
  number-series reuse, and identity authorization reuse.
- Versioned Inventory REST APIs under `/api/v1/inventory`, RFC 9457 Problem Details handling,
  OpenAPI grouping, Flyway migration, jOOQ repository, Spring Modulith and ArchUnit checks, and
  PostgreSQL Testcontainers coverage for P3.4.
- Phase P3.3.5 Shared Product Catalog foundation for product, SKU, product code, GTIN/EAN/UPC,
  barcode, category/brand/family assignments, product attributes and values, UOM assignment,
  packaging hierarchy, units per package, dimensions, weights, media, images, documents, manuals,
  brochures, lifecycle status, multilingual content, tags, search metadata, and warranty metadata.
- Versioned Product Catalog REST APIs under `/api/v1/product-catalog`, RFC 9457 Problem Details
  handling, OpenAPI grouping, Flyway migration, jOOQ repository, Spring Modulith and ArchUnit
  checks, audit/attachment/localization ports, and PostgreSQL Testcontainers coverage for P3.3.5.
- Phase P3.3 Master Data foundation for organization, company, business unit, branch, warehouse
  structure references, geography, currencies, exchange rates, units of measure, tax references,
  payment/shipping terms, languages, time zones, fiscal calendars, number series, document types,
  attachment categories, product classification references, attributes, attribute values, and
  barcode types.
- Versioned Master Data REST APIs under `/api/v1/master-data`, RFC 9457 Problem Details handling,
  OpenAPI grouping, Flyway migration, jOOQ repository, Spring Modulith and ArchUnit checks, and
  PostgreSQL Testcontainers coverage for P3.3.
- Phase P3.2.5 Platform Foundation for internal domain events, outbox, audit records, background
  jobs, scheduler-ready records, file storage abstraction, attachments, configuration/global
  settings, feature flags, cache abstraction, localization, error catalog, and domain event catalog.
- Versioned Platform REST APIs under `/api/v1/platform`, RFC 9457 Problem Details handling, OpenAPI
  grouping, Flyway migration, jOOQ repository, Spring Modulith and ArchUnit checks, and PostgreSQL
  Testcontainers coverage for P3.2.5.
- Phase P3.2 Identity and Access foundation for users, roles, permissions, scoped assignments,
  sessions, refresh tokens, password credentials, and capability decisions.
- Spring Security integration, JWT access-token issuance, refresh-token rotation, Argon2 password
  hashing, account-lock policy, password-change flow, and session revocation.
- Versioned Identity REST APIs under `/api/v1/auth`, `/api/v1/identity`, and
  `/api/v1/access-control`, plus RFC 9457 Problem Details handling and OpenAPI grouping.
- IAM-only Flyway migration, jOOQ repository, Spring Modulith boundary verification, ArchUnit
  checks, PostgreSQL Testcontainers coverage, and frontend contract tests for P3.2.
- Static Identity and Access administration page covering login, users, roles, permissions,
  assignments, profile, sessions, and change password with RTL/LTR readiness.
- Phase P3.1 Enterprise Structure foundation for enterprise, legal entity, company, branch,
  warehouse, zone, and location lifecycle management.
- Versioned Enterprise Structure REST API, Problem Details error handling, OpenAPI grouping, and
  static API contract page.
- Flyway migration, jOOQ repository, Spring Modulith boundary verification, ArchUnit checks,
  PostgreSQL Testcontainers coverage, and frontend contract tests for P3.1.
- Phase P2 business architecture documentation baseline.
- Capability map, bounded contexts, context map, master-data architecture, process maps, event
  catalog, permission model, numbering and status architecture, reporting map, integration map, and
  control documents.
- Phase P1 repository foundation.
- Repository governance, standards, ADRs, runbooks, and contribution templates.
- Gradle, pnpm, Nx, Java, Node.js, TypeScript, linting, formatting, and test foundations.
- Continuous integration, security scanning, license reporting, and SBOM generation.

[Unreleased]: https://github.com/Mehdi3006/newland-erp/compare/main...HEAD
