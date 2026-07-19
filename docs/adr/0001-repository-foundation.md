# ADR-0001: Establish the enterprise monorepo foundation

- Status: Accepted
- Date: 2026-07-19
- Owners: `@Mehdi3006`
- Deciders: `@Mehdi3006`
- Consulted: Architecture report reviewers
- Supersedes: None
- Superseded by: None

## Context

Newland ERP needs a durable repository foundation before any business capability is implemented.
Future teams will span JVM services, TypeScript applications, shared contracts, platform assets, and
engineering automation. The foundation must provide consistent ownership, dependency management,
quality gates, security evidence, and architectural traceability without prejudging business
modules.

## Decision drivers

- Long support horizons and predictable upgrades.
- Strong static typing across backend and frontend work.
- Reproducible builds on developer machines and CI.
- Selective task execution as the repository grows.
- Explicit architecture boundaries and ownership.
- Supply-chain security and auditable release inputs.

## Considered options

1. Gradle plus pnpm workspaces orchestrated by Nx.
2. Maven plus npm workspaces and independent scripts.
3. Bazel as the single polyglot build system.
4. Multiple repositories organized by deployable.
5. A single framework-generated application repository.

## Decision

Use a polyglot monorepo with:

- Gradle Wrapper and version catalogs for JVM builds;
- Java 25 as the JVM toolchain baseline;
- pnpm workspaces for JavaScript dependency installation;
- Nx for the repository task graph and affected execution;
- TypeScript 6, ESLint 10, Prettier 3, and Vitest for TypeScript tooling;
- Spotless and Checkstyle for JVM formatting and static style;
- JUnit and an ArchUnit convention for future JVM tests;
- Playwright configured but test-free until a web application is approved;
- GitHub Actions for quality, security, licensing, SBOM, and architecture gates;
- ADRs and CODEOWNERS for decision and ownership traceability.

Top-level directories define ownership boundaries:

- `apps/` for future deployables;
- `libs/` for future reusable code;
- `contracts/` for future explicit cross-boundary schemas;
- `platform/` for future delivery and runtime-platform assets;
- `tools/` for repository automation;
- `docs/` for governed documentation.

Phase P1 permits only documentation and tooling in these boundaries. It does not permit an
application, database, endpoint, page, entity, or ERP behavior.

## Consequences

### Positive

- One review and governance surface supports cross-cutting changes.
- Wrapper and lock files make tool resolution reproducible.
- Nx can scale from one foundation project to an affected project graph.
- Gradle remains idiomatic for future JVM development.
- Supply-chain artifacts and policies exist before production code.

### Negative

- Contributors must understand two package ecosystems.
- Nx and Gradle task ownership must remain clearly separated.
- Java toolchain provisioning and Node version management add bootstrap cost.
- A monorepo requires disciplined boundaries to avoid accidental coupling.

### Neutral or follow-up

- The first runtime module requires its own architecture approval.
- Module templates and application framework choices remain future decisions.
- Remote build caching is deferred until scale justifies its operational cost.

## Rejected alternatives

### Maven

Maven is mature but offers less expressive convention composition and incremental multi-project
configuration for the intended JVM platform. Gradle's toolchains, version catalog, and convention
mechanisms better fit a growing monorepo.

### npm or Yarn workspaces

npm is widely available, but pnpm's content-addressed store, strict dependency layout, and workspace
controls reduce disk usage and undeclared dependency access. Yarn Plug'n'Play can be effective but
has a larger compatibility and onboarding surface for mixed tooling.

### Bazel

Bazel can provide excellent hermetic, large-scale builds. It was rejected for P1 because it would
impose substantial rule authoring and specialist knowledge before repository scale demonstrates the
need. Native Gradle and pnpm workflows are easier for future teams to operate.

### Multiple repositories

Multiple repositories simplify some local checkouts but make atomic contract changes, policy
enforcement, and dependency visibility harder. Newland ERP benefits more from shared governance and
an explicit project graph at this stage.

### Framework-generated starter applications

A Spring Boot or React starter would prematurely decide deployment and UI architecture and would
violate the approved P1 boundary. Framework selection belongs to the ADR for the first approved
runtime.

## Validation

The decision is validated by reproducible clean installs, passing Gradle and pnpm checks, an
explicit Nx project graph, architecture boundary verification, and successful CI security and SBOM
workflows. Reconsider the orchestration choice if repository scale, build time, or language
diversity makes the current model operationally unsustainable.
