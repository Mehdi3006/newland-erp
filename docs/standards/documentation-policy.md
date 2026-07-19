# Documentation Policy

Documentation is part of the change, not a follow-up artifact.

## Placement

- Root files explain repository-wide behavior.
- `docs/adr` records durable architecture decisions.
- `docs/architecture` describes the current architecture.
- `docs/standards` contains mandatory policy.
- `docs/runbooks` contains executable procedures.
- `docs/decisions` contains bounded non-architecture choices.
- Module READMEs explain local purpose, ownership, public API, and operation.

## Quality

Documents must name the audience, use plain language, contain tested commands, and link to canonical
sources. Prefer diagrams only when relationships are materially clearer than prose. Use relative
links for repository content and HTTPS links for external sources.

## Lifecycle

The owner reviews a document when its subject changes and at least annually for operational or
policy content. Outdated material is corrected or explicitly marked historical. Do not delete
accepted ADRs; supersede them.

## Sensitive content

Never record credentials, private customer information, security exploit details awaiting
disclosure, or production datasets. Examples must be structural and domain-neutral during P1.
