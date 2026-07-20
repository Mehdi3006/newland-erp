# Next Codex Task

## Recommended Task: Complete P3.2.5 Platform Foundation Review

P3.2 is approved and merged. P3.2.5 implementation exists on branch
`codex/p3-2-5-platform-foundation` and must receive explicit architectural approval before any
Inventory or other business-module work begins.

The next task is final review, validation, and approval handling for Platform Foundation only.

## P3.2.5 Scope

Review and validate implementation for:

- Internal Domain Event Bus.
- Outbox Pattern.
- Audit Infrastructure.
- Background Job Framework and scheduler-ready records.
- File Storage Abstraction.
- Attachment Framework.
- Configuration Service and Global Settings.
- Feature Flags.
- Cache Abstraction.
- Localization Infrastructure.
- Error Catalog.
- Domain Event Catalog.
- Shared Platform APIs.

## P3.2.5 Rules

- Do not implement Inventory, Master Data, Sales, Procurement, Accounting, CRM, HR, Reporting,
  Workflow, or AI in P3.2.5.
- Do not add external brokers, Redis, Kafka, RabbitMQ, email, SMS, or WhatsApp providers.
- Keep all infrastructure replaceable behind ports and adapters.
- Do not begin Inventory before P3.2.5 approval.

## P3.2.5 Acceptance Draft

- Platform infrastructure model passes review.
- Outbox, audit, jobs, storage, configuration, flags, cache, localization, and catalog contracts
  pass review.
- API, persistence, migrations, architecture checks, security checks, and tests pass quality gates.
- No business modules implemented.
