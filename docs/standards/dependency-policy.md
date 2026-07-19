# Dependency Policy

## Selection

A dependency must have a clear owner, maintained release history, compatible license, security
posture, and measurable benefit over local implementation. Prefer stable releases and
platform-native capabilities. Do not add two libraries for the same responsibility without
documenting the distinction.

## Declaration and locking

- JVM versions belong in `gradle/libs.versions.toml`.
- JVM project configurations use Gradle strict dependency locking.
- JavaScript versions are exact in `package.json` and resolved in `pnpm-lock.yaml`.
- GitHub Actions from third parties are pinned to immutable commit SHAs with a version comment.
- Dynamic versions, unbounded ranges, Git URLs, and unreviewed local patches are prohibited.
- Production code must not rely on undeclared transitive dependencies.

## Review

Dependency pull requests must include purpose, license, current version, transitive impact, security
findings, and rollback approach. Major upgrades require migration notes and focused testing.

## Maintenance

Review dependencies at least monthly and critical advisories immediately. Merge patches after checks
and owner review. Group low-risk tooling patches only when failures remain attributable. Remove
unused dependencies promptly.

Exceptions require an owner, expiry date, compensating control, and decision record.
