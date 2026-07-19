# Review Policy

## Required review

Every non-emergency change to `main` requires a pull request and at least one approval from a code
owner. Require two approvals for:

- authentication, authorization, cryptography, or secret handling;
- dependency or workflow permission changes;
- public contracts or data migrations;
- repository governance and branch protection;
- accepted ADRs and architecture-boundary changes.

At least one approval must come from the affected owner. Authors cannot approve their own changes.

## Reviewer responsibilities

Review for correctness, scope, security, privacy, operability, maintainability, tests,
documentation, compatibility, and dependency direction. Distinguish blocking findings from
suggestions. Resolve every blocking thread before merge.

## Automation-authored changes

Automation may prepare changes but does not replace accountable review. Generated changes must
identify the generator, remain reproducible, and receive the same owner approval as human-authored
changes.

## Emergency changes

An emergency fix may use an expedited review by one authorized maintainer when delay creates greater
risk. Record the reason, evidence, and rollback. A normal retrospective review and missing tests or
documentation must follow within two business days.
