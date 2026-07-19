# Repository Governance

## Roles

- **Repository owner:** administers access, branch protection, and releases.
- **Architecture owner:** accepts or rejects architecture decisions.
- **Path owner:** maintains a governed directory.
- **Security owner:** assesses vulnerabilities and security-sensitive changes.
- **Contributor:** proposes focused, tested, documented changes.

One person may hold several roles during early development, but decisions and reviews remain
explicit.

## Decision hierarchy

1. Applicable law, contractual obligations, and organization security policy.
2. Accepted ADRs.
3. Repository standards.
4. Architecture documentation.
5. Module documentation.
6. Implementation choices.

A lower level may not contradict a higher level.

## Exceptions

An exception states the violated rule, reason, scope, owner, risk, compensating control, expiry
date, and remediation issue. Permanent exceptions to architecture require an ADR. Expired exceptions
fail review.

## Policy changes

Governance changes use a dedicated pull request, explain migration impact, and require
repository-owner approval. Changes must not silently weaken protection or ownership.
