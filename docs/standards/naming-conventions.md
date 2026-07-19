# Naming Conventions

| Item                            | Convention                  | Example                    |
| ------------------------------- | --------------------------- | -------------------------- |
| Directories and package names   | kebab-case                  | `identity-access`          |
| Java packages                   | lowercase reverse domain    | `com.newlanderp.identity`  |
| Java and Kotlin types           | PascalCase                  | `AccessPolicy`             |
| Java and Kotlin members         | camelCase                   | `evaluatePolicy`           |
| TypeScript files                | kebab-case                  | `access-policy.ts`         |
| TypeScript types and classes    | PascalCase                  | `AccessPolicy`             |
| TypeScript values and functions | camelCase                   | `evaluatePolicy`           |
| Constants                       | ecosystem idiom             | `MAX_RETRIES`              |
| Environment variables           | uppercase snake case        | `NEWLAND_LOG_LEVEL`        |
| Git branches                    | category plus kebab-case    | `feat/access-policy`       |
| ADRs                            | four digits plus kebab-case | `0002-runtime-boundary.md` |

Names must describe business or technical meaning without abbreviations that are only understood by
one team. Avoid generic buckets such as `common`, `helpers`, `misc`, and `utils`; use a
capability-specific name.

Reserved top-level directories may not be renamed or extended without an ADR.
