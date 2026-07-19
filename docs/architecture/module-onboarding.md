# Future Module Onboarding

No runtime or business module may be added during Phase P1. When a later phase authorizes one, use
this sequence.

## Approval gate

1. Define the capability, owner, data sensitivity, and deployment boundary.
2. Identify upstream and downstream dependencies.
3. Propose an ADR covering framework, interface, persistence, security, and operational
   consequences.
4. Obtain architecture and affected-owner approval before scaffolding code.

## Repository integration

1. Choose exactly one top-level location:
   - `apps/<name>` for a deployable;
   - `libs/<name>` for reusable code;
   - `contracts/<name>` for a cross-boundary schema;
   - `platform/<name>` for delivery infrastructure.
2. Add a module README containing purpose, owner, public API, dependencies, data classification,
   build commands, and support expectations.
3. Add an Nx `project.json` or an inference-compatible project configuration.
4. For JVM modules, include the project from `settings.gradle.kts` and apply the approved Java or
   Kotlin conventions.
5. Generate and review Gradle dependency locks for the new project.
6. Add unit tests, architecture tests, ownership rules, and dependency constraints.
7. Update architecture maps and the file catalog.

## Acceptance gate

A module is not accepted until formatting, linting, compilation, unit tests, architecture
verification, dependency scanning, license reporting, and SBOM generation pass. Deployables
additionally need threat modeling, operational runbooks, observability, rollback, and release
approval.
