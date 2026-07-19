# Directory Ownership

The GitHub-enforced source of truth is `.github/CODEOWNERS`. This document adds responsibility and
review context.

| Path                  | Current owner | Responsibility                             |
| --------------------- | ------------- | ------------------------------------------ |
| `/`                   | `@Mehdi3006`  | Repository governance and shared toolchain |
| `/.github/`           | `@Mehdi3006`  | Collaboration policy and CI security       |
| `/apps/`              | `@Mehdi3006`  | Future deployable boundary                 |
| `/config/`            | `@Mehdi3006`  | Static-analysis policy                     |
| `/contracts/`         | `@Mehdi3006`  | Future cross-boundary contracts            |
| `/docs/adr/`          | `@Mehdi3006`  | Architecture decisions                     |
| `/docs/architecture/` | `@Mehdi3006`  | Current architecture                       |
| `/docs/runbooks/`     | `@Mehdi3006`  | Operational procedures                     |
| `/docs/standards/`    | `@Mehdi3006`  | Engineering standards                      |
| `/gradle/`            | `@Mehdi3006`  | JVM build foundation                       |
| `/libs/`              | `@Mehdi3006`  | Future shared-library boundary             |
| `/platform/`          | `@Mehdi3006`  | Future platform boundary                   |
| `/tools/`             | `@Mehdi3006`  | Repository automation                      |

Replace individual fallback ownership with resilient teams when teams are created. No path may
become unowned during that transition.
