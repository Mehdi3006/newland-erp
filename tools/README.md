# Repository Tools

This directory contains automation that operates on the repository itself. It must not become a
runtime dependency and must not contain ERP behavior.

- `architecture/` verifies required foundation files and P1 boundaries.
- `compliance/` enforces repository-level dependency policy.
- Future `ci/` or `release/` tools must be deterministic, tested, documented, and owned.

Scripts use Node.js ESM so they run through the repository's declared Node toolchain without an
additional runtime.
