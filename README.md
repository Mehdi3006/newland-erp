# Newland ERP

Newland ERP is an enterprise resource planning platform under staged architectural development. This
repository currently contains only the Phase P1 repository foundation: governance, documentation,
toolchains, quality controls, and continuous integration.

No ERP business module, user interface, service, database, API, or sample business data exists in
this phase.

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

## Repository map

```text
.
├── .github/        GitHub governance, templates, and workflows
├── apps/           Reserved deployable-application boundary
├── config/         Shared static-analysis configuration
├── contracts/      Reserved cross-boundary contract definitions
├── docs/           Architecture, standards, decisions, and runbooks
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

Business implementation is explicitly outside Phase P1.

## Collaboration

Read [`CONTRIBUTING.md`](CONTRIBUTING.md), the [`docs/standards`](docs/standards/README.md) index,
and [`SECURITY.md`](SECURITY.md) before contributing. Changes are licensed under the
[MIT License](LICENSE).
