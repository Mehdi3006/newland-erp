# Foundation File Catalog

This catalog explains the purpose of every Phase P1 file. Generated build output is excluded because
it is not versioned.

## Root governance and developer experience

| File                       | Why it exists                                                                                  |
| -------------------------- | ---------------------------------------------------------------------------------------------- |
| `README.md`                | Entry point, scope boundary, toolchain summary, repository map, and bootstrap commands         |
| `LICENSE`                  | Grants the approved MIT permissions and conditions                                             |
| `SECURITY.md`              | Defines private vulnerability reporting and response expectations                              |
| `CONTRIBUTING.md`          | Defines the contributor workflow and change acceptance criteria                                |
| `CHANGELOG.md`             | Records notable changes using Keep a Changelog and release links                               |
| `.editorconfig`            | Normalizes encoding, line endings, indentation, and final newlines across editors              |
| `.gitattributes`           | Normalizes Git text handling and marks binary or generated files                               |
| `.gitignore`               | Prevents local state, credentials, dependencies, and generated output from entering Git        |
| `.dockerignore`            | Prevents irrelevant or sensitive files from entering a future container build context          |
| `.npmrc`                   | Enforces exact packages, strict peers, engine versions, and a package-release cooling period   |
| `.nvmrc`                   | Selects the exact Node.js version for nvm-compatible tools                                     |
| `.node-version`            | Selects the same Node.js version for cross-tool version managers                               |
| `.java-version`            | Selects the Java 25 toolchain for JDK version managers                                         |
| `.prettierignore`          | Keeps generated and tool-owned files out of Prettier                                           |
| `.prettierrc.json`         | Defines repository-wide text and TypeScript formatting                                         |
| `.markdownlint-cli2.jsonc` | Defines Markdown quality rules and included documents                                          |
| `mise.toml`                | Offers one-command Java, Node.js, and pnpm activation without becoming mandatory               |
| `Makefile`                 | Provides discoverable aliases for bootstrap, verification, formatting, testing, and SBOM tasks |
| `.vscode/extensions.json`  | Recommends the editor integrations used by repository standards                                |
| `.vscode/settings.json`    | Aligns VS Code formatting and Java project behavior with repository tools                      |

## GitHub governance and automation

| File                                             | Why it exists                                                                   |
| ------------------------------------------------ | ------------------------------------------------------------------------------- |
| `.github/CODEOWNERS`                             | Requests accountable reviews for every maintained and reserved path             |
| `.github/pull_request_template.md`               | Collects scope, verification, risk, ownership, and security evidence            |
| `.github/ISSUE_TEMPLATE/config.yml`              | Disables unstructured issues and directs security reports privately             |
| `.github/ISSUE_TEMPLATE/bug-report.yml`          | Collects safe, reproducible defect reports                                      |
| `.github/ISSUE_TEMPLATE/change-request.yml`      | Collects desired outcomes, boundaries, and acceptance criteria                  |
| `.github/ISSUE_TEMPLATE/architecture-change.yml` | Requires options, drivers, owners, and validation before architecture work      |
| `.github/dependabot.yml`                         | Schedules bounded dependency and workflow update proposals                      |
| `.github/workflows/ci.yml`                       | Runs clean Node and Gradle formatting, lint, build, and test gates              |
| `.github/workflows/architecture.yml`             | Runs the independently visible Phase P1 boundary check                          |
| `.github/workflows/security.yml`                 | Scans Git history for secrets and audits the JavaScript lock file               |
| `.github/workflows/osv-scanner.yml`              | Scans supported manifests and lock files against the OSV vulnerability database |
| `.github/workflows/supply-chain.yml`             | Produces license inventories and JVM and repository SBOM artifacts              |

Third-party actions are pinned to immutable commit SHAs. Version comments keep the reviewed release
visible to maintainers and update automation.

## JVM foundation

| File                                       | Why it exists                                                                                             |
| ------------------------------------------ | --------------------------------------------------------------------------------------------------------- |
| `settings.gradle.kts`                      | Names the build, locks repositories, provisions toolchains, and reserves explicit future module inclusion |
| `build.gradle.kts`                         | Defines shared Java toolchain, JUnit, ArchUnit, Checkstyle, Spotless, lifecycle, and SBOM conventions     |
| `gradle.properties`                        | Defines reproducible Gradle performance, encoding, warning, and Java toolchain settings                   |
| `gradle/libs.versions.toml`                | Centralizes JVM library and plugin versions                                                               |
| `gradle/verification-metadata.xml`         | Pins SHA-256 checksums for resolved Gradle plugin and build dependencies                                  |
| `gradlew`                                  | Runs the reviewed Gradle version on Unix-like systems                                                     |
| `gradlew.bat`                              | Runs the reviewed Gradle version on Windows                                                               |
| `gradle/wrapper/gradle-wrapper.jar`        | Bootstraps Gradle without a global installation                                                           |
| `gradle/wrapper/gradle-wrapper.properties` | Pins the distribution URL, network policy, and official SHA-256 checksum                                  |
| `config/checkstyle/checkstyle.xml`         | Defines the baseline Java static-style rules                                                              |
| `config/checkstyle/suppressions.xml`       | Provides a reviewed, currently empty suppression surface                                                  |
| `config/checkstyle/README.md`              | Defines how Checkstyle exceptions are governed                                                            |
| `config/detekt/detekt.yml`                 | Reserves a strict baseline for a future approved Kotlin module                                            |
| `config/detekt/README.md`                  | Explains why Detekt is versioned but not yet applied                                                      |
| `config/archunit/README.md`                | Defines the ArchUnit activation and boundary-testing expectations                                         |

## JavaScript and monorepo foundation

| File                   | Why it exists                                                                            |
| ---------------------- | ---------------------------------------------------------------------------------------- |
| `package.json`         | Declares exact tool dependencies, supported engines, and canonical repository commands   |
| `pnpm-lock.yaml`       | Makes the complete JavaScript dependency graph reproducible and reviewable               |
| `pnpm-workspace.yaml`  | Defines future workspace boundaries, supply-chain delay, and lifecycle-script allowlist  |
| `nx.json`              | Defines shared inputs, cache behavior, task dependencies, and the `main` comparison base |
| `project.json`         | Gives repository tooling an explicit Nx project and quality targets                      |
| `tsconfig.base.json`   | Defines strict TypeScript defaults inherited by future projects                          |
| `tsconfig.json`        | Type-checks repository automation and test configuration                                 |
| `eslint.config.mjs`    | Defines flat ESLint configuration for typed TypeScript and Node.js scripts               |
| `vitest.config.ts`     | Defines deterministic Node.js tests and coverage output                                  |
| `playwright.config.ts` | Reserves safe browser-test defaults without creating a web application                   |
| `tests/e2e/README.md`  | Documents why Playwright has no executable tests in P1                                   |

Nx does not require `workspace.json` in this repository. Current Nx uses `project.json` and plugin
inference; adding a redundant legacy workspace file would create a second project-configuration
source of truth.

## Repository boundaries and automation

| File                                  | Why it exists                                                           |
| ------------------------------------- | ----------------------------------------------------------------------- |
| `apps/README.md`                      | Reserves and constrains the future deployable boundary                  |
| `libs/README.md`                      | Reserves and constrains the future reusable-library boundary            |
| `contracts/README.md`                 | Reserves and constrains the future contract boundary                    |
| `platform/README.md`                  | Reserves and constrains the future delivery-platform boundary           |
| `tools/README.md`                     | Separates repository automation from future runtime code                |
| `tools/architecture/verify.mjs`       | Machine-checks required files and rejects P1 runtime artifacts          |
| `tools/architecture/verify.d.mts`     | Supplies strict TypeScript declarations for the ESM verification module |
| `tools/architecture/verify.test.ts`   | Tests accepted and rejected P1 architecture paths                       |
| `tools/compliance/README.md`          | Explains repository compliance automation and its limits                |
| `tools/compliance/check-licenses.mjs` | Rejects dependency licenses that require explicit legal approval        |

## Documentation

| File or collection                                 | Why it exists                                                              |
| -------------------------------------------------- | -------------------------------------------------------------------------- |
| `docs/README.md`                                   | Routes readers to the correct governed documentation type                  |
| `docs/adr/README.md`                               | Defines when and how to write, accept, and supersede ADRs                  |
| `docs/adr/0000-template.md`                        | Provides a complete, repeatable ADR structure                              |
| `docs/adr/0001-repository-foundation.md`           | Records the chosen stack, consequences, and rejected alternatives          |
| `docs/architecture/README.md`                      | Indexes current architecture documentation                                 |
| `docs/architecture/repository-layout.md`           | Defines directory responsibilities and dependency direction                |
| `docs/architecture/module-onboarding.md`           | Defines the approval, integration, and acceptance gates for future modules |
| `docs/architecture/quality-gates.md`               | Maps commands to local and CI enforcement                                  |
| `docs/architecture/phase-p1-scope.md`              | Makes P1 inclusions, exclusions, and exit criteria explicit                |
| `docs/architecture/foundation-dependency-graph.md` | Shows the order and dependencies among foundation capabilities             |
| `docs/architecture/foundation-risks.md`            | Tracks repository-foundation risks and mitigations                         |
| `docs/decisions/README.md`                         | Distinguishes bounded decisions from architecture decisions                |
| `docs/decisions/decision-template.md`              | Provides owner, expiry, consequence, and follow-up fields                  |
| `docs/runbooks/README.md`                          | Indexes executable repository procedures                                   |
| `docs/runbooks/local-development.md`               | Provides clean bootstrap, verification, and troubleshooting                |
| `docs/runbooks/dependency-upgrade.md`              | Provides controlled JavaScript and JVM upgrade steps                       |
| `docs/runbooks/release.md`                         | Reserves a governed future release sequence                                |
| `docs/runbooks/security-response.md`               | Defines private triage, containment, remediation, and disclosure           |
| `docs/standards/README.md`                         | Indexes mandatory standards                                                |
| `docs/standards/repository-standards.md`           | Defines repository invariants and build ownership                          |
| `docs/standards/coding-standards.md`               | Defines maintainability, language, security, and test expectations         |
| `docs/standards/naming-conventions.md`             | Normalizes names across files, code, ADRs, and branches                    |
| `docs/standards/commit-conventions.md`             | Defines Conventional Commit types and breaking-change syntax               |
| `docs/standards/branch-strategy.md`                | Defines short-lived branches, protection, and merge behavior               |
| `docs/standards/versioning-policy.md`              | Defines Semantic Versioning, tags, compatibility, and changelog rules      |
| `docs/standards/dependency-policy.md`              | Governs selection, exact versions, locking, actions, and exceptions        |
| `docs/standards/documentation-policy.md`           | Governs placement, quality, lifecycle, and sensitive content               |
| `docs/standards/review-policy.md`                  | Defines approvals, reviewer duties, automation, and emergencies            |
| `docs/standards/code-ownership.md`                 | Defines owner responsibilities and succession                              |
| `docs/standards/repository-governance.md`          | Defines roles, decision hierarchy, exceptions, and policy changes          |
| `docs/standards/directory-ownership.md`            | Maps paths to current accountable owners                                   |
| `docs/standards/github-labels.md`                  | Defines the label names, colors, meanings, and administration policy       |
