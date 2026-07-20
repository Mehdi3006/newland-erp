# Newland ERP Business Architecture

Status: Draft for Phase P2 acceptance  
Owner: Business Architecture  
Review cadence: Every implementation phase gate

This documentation defines the business architecture that future Newland ERP implementation must
follow. It is intentionally documentation-only. It does not approve backend code, frontend code,
database migrations, API implementation, UI pages, workflows, or operational sample data.

## Scope

Phase P2 establishes:

- Business context and open decisions.
- Enterprise capability map.
- Bounded contexts and context relationships.
- Organization hierarchy and scope rules.
- Master-data ownership.
- End-to-end process maps.
- Domain language.
- Domain event catalog.
- Permission model.
- Numbering and status architecture.
- Reporting and integration maps.
- Acceptance criteria and implementation control documents.

## Navigation

| Area                  | Document                                                                   |
| --------------------- | -------------------------------------------------------------------------- |
| Project context       | [`00-project-context/README.md`](00-project-context/README.md)             |
| Capability map        | [`01-capability-map/README.md`](01-capability-map/README.md)               |
| Domain map            | [`02-domain-map/README.md`](02-domain-map/README.md)                       |
| Context map           | [`03-context-map/README.md`](03-context-map/README.md)                     |
| Organization model    | [`04-organization-model/README.md`](04-organization-model/README.md)       |
| Master data           | [`05-master-data/README.md`](05-master-data/README.md)                     |
| Transaction domains   | [`06-transaction-domains/README.md`](06-transaction-domains/README.md)     |
| Financial domains     | [`07-financial-domains/README.md`](07-financial-domains/README.md)         |
| Control domains       | [`08-control-domains/README.md`](08-control-domains/README.md)             |
| Cross-cutting domains | [`09-cross-cutting-domains/README.md`](09-cross-cutting-domains/README.md) |
| Process map           | [`10-process-map/README.md`](10-process-map/README.md)                     |
| Event catalog         | [`11-event-catalog/README.md`](11-event-catalog/README.md)                 |
| Permission model      | [`12-permission-model/README.md`](12-permission-model/README.md)           |
| Numbering and status  | [`13-numbering-and-status/README.md`](13-numbering-and-status/README.md)   |
| Reporting map         | [`14-reporting-map/README.md`](14-reporting-map/README.md)                 |
| Integration map       | [`15-integration-map/README.md`](15-integration-map/README.md)             |
| Acceptance            | [`16-acceptance/README.md`](16-acceptance/README.md)                       |
| Decisions             | [`17-decisions/README.md`](17-decisions/README.md)                         |

## Architecture Rules

- Mark missing facts as `OPEN DECISION`.
- Do not invent legal, tax, staffing, pricing, or operational-volume facts.
- Define ownership before implementation.
- Prefer bounded contexts over menu structures.
- Avoid circular dependencies.
- Use capability permissions instead of hard-coded role behavior.
- Treat accounting and inventory ledgers as immutable after posting.
- Defer transport technology, database design, UI design, and runtime deployment choices.
