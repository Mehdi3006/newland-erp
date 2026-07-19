# Repository Standards

## Core invariants

- `main` is releasable and protected.
- Builds use checked-in wrappers and lock files.
- Dependencies are declared explicitly and centrally where supported.
- Generated output, credentials, and local state are not committed.
- Every maintained path has an accountable owner.
- Architecture boundaries are machine-checked where practical.
- Documentation changes accompany behavior or policy changes.
- A pull request must not combine unrelated refactoring and feature work.

## Build ownership

Gradle owns JVM dependency resolution, compilation, JVM tests, and JVM SBOM generation. pnpm owns
JavaScript dependency installation. Nx owns the cross-project task graph and affected execution.
Scripts must not create a second source of truth for another tool's responsibility.

## Reproducibility

CI uses `pnpm install --frozen-lockfile` and the checked-in Gradle Wrapper. Developers must not rely
on globally installed project dependencies. Toolchain versions are explicit. Network-resolved or
generated changes must be reviewed and committed when they are part of the build contract.

## Repository cleanliness

Before requesting review:

```bash
pnpm check
./gradlew check
git status --short
```

The final command must show only intentional changes.
