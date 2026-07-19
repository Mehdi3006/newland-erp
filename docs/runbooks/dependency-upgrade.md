# Dependency Upgrade Runbook

- Owner: Repository owner
- Last reviewed: 2026-07-19

## Prepare

1. Read release notes, migration guides, advisories, and license changes.
2. Confirm the target version supports the repository's Java and Node toolchains.
3. Open a focused branch and record the reason for the upgrade.

## JavaScript dependencies

Update exact versions in `package.json`, then:

```bash
pnpm install
pnpm check
pnpm audit --audit-level high
```

Review both manifest and lock-file changes. Investigate new lifecycle scripts, native binaries,
registries, and transitive licenses.

## JVM dependencies and plugins

Update `gradle/libs.versions.toml` or the wrapper properties, then:

```bash
./gradlew check
./gradlew dependencies
./gradlew cyclonedxBom
```

For a wrapper upgrade, run the wrapper task twice so scripts, JAR, properties, and checksum are
consistent.

## Validate and roll back

Run the complete CI-equivalent checks and inspect generated dependency, license, and SBOM artifacts.
The rollback is a revert of the focused upgrade commit. Major upgrades include migration notes and
require affected-owner approval.
