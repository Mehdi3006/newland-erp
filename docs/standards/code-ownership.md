# Code Ownership

`CODEOWNERS` identifies required reviewers; it does not transfer accountability from contributors or
reviewers.

## Owner responsibilities

Owners:

- maintain architecture and quality within their paths;
- review changes within the service-level expectation;
- triage vulnerabilities and dependency updates;
- keep documentation and runbooks current;
- identify a successor before relinquishing ownership;
- escalate cross-boundary decisions to architecture governance.

## Ownership model

Prefer team ownership over individuals as the project grows. During foundation, the repository owner
is the fallback owner. A path may have multiple owners for resilience. Security-sensitive paths
should include a security owner when that role exists.

Every newly approved module must add both a `CODEOWNERS` rule and an entry in
[`directory-ownership.md`](directory-ownership.md) in the same pull request.
