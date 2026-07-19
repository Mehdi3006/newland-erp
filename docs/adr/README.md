# Architecture Decision Records

ADRs preserve the context, decision, consequences, and alternatives for architecture choices that
are difficult or expensive to reverse.

## When an ADR is required

Create an ADR for:

- a new deployable or independently owned module;
- a change to a top-level repository boundary;
- a new persistence, messaging, security, API, or integration pattern;
- a change to a supported language, framework, build system, or quality gate;
- an exception to a repository-wide architecture rule.

Routine dependency patches, implementation details within an approved boundary, and temporary
delivery choices normally use a decision record or pull request.

## Workflow

1. Copy [`0000-template.md`](0000-template.md).
2. Assign the next four-digit number and a short kebab-case title.
3. Set status to `Proposed` and open a pull request.
4. Obtain architecture and affected-owner approval.
5. Change status to `Accepted`, `Rejected`, or `Deferred`.
6. If a later decision replaces it, keep the original file and mark it `Superseded by ADR-NNNN`.

Accepted ADRs are immutable except for status, corrected links, and explicit supersession metadata.
A material change requires a new ADR.
