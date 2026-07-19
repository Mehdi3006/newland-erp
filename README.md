# Newland ERP

Newland ERP is an enterprise resource planning platform under staged architectural development. The
repository contains the completed Phase P1 repository foundation and the Phase P2 business
architecture documentation baseline.

No runtime ERP implementation exists yet. There is no ERP business module, user interface, service,
database migration, API, controller, page, dashboard, workflow implementation, or sample business
data in this repository.

## Current phase

- P1 Repository Foundation is complete, approved, closed, and frozen.
- P2 Business Architecture documentation has been added and is under architectural review.
- The next approved gate is P2 architectural approval. P3 must not begin until that approval is
  explicitly granted.

## Foundation stack

| Concern                  | Standard                                                                   |
| ------------------------ | -------------------------------------------------------------------------- |
| Repository orchestration | Nx 23 with pnpm 11 workspaces                                              |
| JVM builds               | Gradle 9.5 wrapper with a Java 25 toolchain                                |
| TypeScript               | TypeScript 6, ESLint 10, Prettier 3                                        |
| JVM quality              | Spotless, Checkstyle, JUnit, and ArchUnit conventions                      |
| JavaScript quality       | ESLint, Vitest, and a Playwright placeholder                               |
| Supply chain             | Dependency scanning, secret scanning, license reports, and CycloneDX SBOMs |
| Automation               | GitHub Actions                                                             |
| Decisions                | Architecture Decision Records (ADRs)                                       |

The selected versions are centralized in [`gradle/libs.versions.toml`](gradle/libs.versions.toml)
and [`package.json`](package.json). The reasoning and rejected alternatives are recorded in
[`docs/adr/0001-repository-foundation.md`](docs/adr/0001-repository-foundation.md).

## Business architecture

Phase P2 adds documentation for the ERP business architecture without adding runtime code. The
business architecture index is
[`docs/business-architecture/README.md`](docs/business-architecture/README.md).

It defines the capability map, bounded contexts, context map, organization model, master-data
architecture, transaction and financial domains, control domains, cross-cutting domains, process
maps, event catalog, permission model, numbering and status architecture, reporting map, integration
map, acceptance criteria, and open architectural decisions.

## Repository map

```text
.
├── .github/        GitHub governance, templates, and workflows
├── apps/           Reserved deployable-application boundary
├── config/         Shared static-analysis configuration
├── contracts/      Reserved cross-boundary contract definitions
├── docs/           Architecture, business architecture, standards, decisions, and runbooks
├── gradle/         Wrapper and centralized JVM dependency versions
├── libs/           Reserved reusable-library boundary
├── platform/       Reserved delivery and runtime-platform boundary
└── tools/          Repository automation and architecture checks
```

Reserved directories contain documentation only. Their presence defines future boundaries; it does
not authorize implementation.

## Prerequisites

- Node.js 24.18.0
- pnpm 11.9.0
- A Java 25 JDK, or network access so Gradle can provision its toolchain
- Git

Version-manager hints are provided in `.nvmrc`, `.node-version`, `.java-version`, and `mise.toml`.

## Bootstrap and verify

```bash
corepack enable
pnpm install --frozen-lockfile
./gradlew --version
pnpm check
./gradlew check
```

Useful commands:

```bash
pnpm format
pnpm test
pnpm test:e2e
pnpm architecture:verify
./gradlew spotlessApply
./gradlew cyclonedxBom
```

`pnpm test:e2e` is intentionally a placeholder with no browser tests until an application is
architecturally approved.

## Adding future modules

Do not add a module by copying a directory ad hoc. First:

1. Obtain approval for the module boundary and record material architecture in an ADR.
2. Select the correct top-level boundary using
   [`docs/architecture/module-onboarding.md`](docs/architecture/module-onboarding.md).
3. Add ownership, build targets, tests, dependency constraints, and documentation in the same pull
   request.
4. Pass every local and CI quality gate.

Business implementation remains outside the current approved scope. P2 documents architecture only;
it does not authorize runtime implementation.

## Collaboration

Read [`CONTRIBUTING.md`](CONTRIBUTING.md), the [`docs/standards`](docs/standards/README.md) index,
and [`SECURITY.md`](SECURITY.md) before contributing. Changes are licensed under the
[MIT License](LICENSE).
