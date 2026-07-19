# Local Development Runbook

- Owner: Repository owner
- Applies to: Phase P1 repository foundation
- Last reviewed: 2026-07-19

## Prerequisites

Install Git and a supported version manager. The repository declares Node.js 24.18.0, pnpm 11.9.0,
and Java 25. Gradle can provision Java through its toolchain resolver when network policy permits.

## Bootstrap

```bash
git clone https://github.com/Mehdi3006/newland-erp.git
cd newland-erp
corepack enable
pnpm install --frozen-lockfile
./gradlew --version
```

Do not use `npm install`, delete a lock file to solve conflicts, or substitute a globally installed
Gradle.

## Verify

```bash
pnpm check
./gradlew check
git status --short
```

A clean checkout remains clean after checks. Generated reports belong in ignored directories.

## Common failures

### Node version rejected

Activate the version in `.nvmrc`, `.node-version`, or `mise.toml`, then reinstall dependencies.

### Java toolchain unavailable

Install a Java 25 JDK or allow Gradle access to the Foojay toolchain resolver. Confirm with
`./gradlew javaToolchains`.

### Frozen lock file failure

Use the declared pnpm version. If dependencies were intentionally changed, run `pnpm install`,
review the complete lock-file diff, and commit it with `package.json`.

### Formatting failure

Run `pnpm format` and `./gradlew spotlessApply`, then review the resulting diff.

Escalate reproducible bootstrap failures with operating-system, tool-version, and command output;
remove credentials and personal paths first.
