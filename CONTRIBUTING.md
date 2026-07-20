# Contributing to Newland ERP

Newland ERP is developed through reviewed, traceable changes. Phase P1 Repository Foundation is
complete, approved, closed, and frozen. Phase P2 Business Architecture documentation exists and is
under architectural review. Runtime business modules and applications still require explicit
architectural approval. P3 implementation must not begin until P2 is approved.

## Before starting

1. Read the [repository standards](docs/standards/README.md).
2. Search existing issues and ADRs.
3. For architectural or cross-boundary work, open an architecture issue and obtain approval before
   implementation.
4. Branch from an up-to-date `main` using the documented branch naming rules.

## Local setup

```bash
corepack enable
pnpm install --frozen-lockfile
./gradlew --version
pnpm check
./gradlew check
```

Gradle provisions Java 25 when a local matching JDK is unavailable and toolchain download is
permitted.

## Change requirements

- Use Conventional Commits.
- Keep changes small and single-purpose.
- Add or update tests for behavior changes.
- Add an ADR for material architecture decisions.
- Update documentation in the same pull request as the change.
- Add or update `CODEOWNERS` when ownership changes.
- Do not weaken a quality gate without an approved ADR and explicit reviewer agreement.
- Do not introduce business data, secrets, or production credentials.

## Pull requests

Complete the pull request template, link the governing issue or ADR, and include verification
evidence. A pull request is ready to merge only when required checks pass, required owners approve,
conversations are resolved, and the branch is current with `main`.

See the [review policy](docs/standards/review-policy.md) for approval rules and the
[branch strategy](docs/standards/branch-strategy.md) for merge behavior.
