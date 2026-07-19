# Quality Gates

## Local gates

| Command                    | Responsibility                        |
| -------------------------- | ------------------------------------- |
| `pnpm format:check`        | Prettier conformance                  |
| `pnpm lint`                | ESLint and Markdown rules             |
| `pnpm build`               | TypeScript configuration compilation  |
| `pnpm test`                | Vitest repository tests               |
| `pnpm architecture:verify` | Required files and Phase P1 boundary  |
| `pnpm license:check`       | Dependency license denylist           |
| `./gradlew spotlessCheck`  | Gradle and JVM text formatting        |
| `./gradlew check`          | Aggregate JVM foundation verification |
| `./gradlew cyclonedxBom`   | CycloneDX JVM SBOM                    |

## CI gates

The pull request pipeline repeats clean-install lint, build, test, Gradle, and architecture checks.
Separate workflows scan dependencies and secrets, enforce the license denylist, preserve license
inventories, and generate SBOM artifacts.

The Playwright configuration is deliberately excluded from required CI execution until a browser
application exists. Its placeholder command succeeds only when no tests are present; future UI
approval must convert it into a required target.

## Gate changes

Removing, suppressing, or downgrading a required gate is an architecture change. It requires an ADR
or a documented emergency exception with an expiry date, owner, compensating control, and follow-up
issue.
