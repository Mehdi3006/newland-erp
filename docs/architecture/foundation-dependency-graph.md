# Foundation Dependency Graph

Phase P1 capabilities are intentionally ordered so later controls rely on stable earlier contracts.

```mermaid
flowchart TD
  A["Existing GitHub main branch"] --> B["Governance and ownership"]
  B --> C["Repository boundaries and documentation"]
  C --> D["Pinned Java and Node toolchains"]
  D --> E["Gradle and pnpm dependency foundations"]
  E --> F["Nx task graph and TypeScript configuration"]
  E --> G["JVM quality conventions"]
  F --> H["ESLint, Prettier, Vitest, and Playwright placeholder"]
  C --> I["Architecture boundary verifier"]
  G --> J["Continuous integration"]
  H --> J
  I --> J
  E --> K["Dependency and secret scanning"]
  E --> L["License inventory and SBOM generation"]
  J --> M["Protected, reviewable future module onboarding"]
  K --> M
  L --> M
```

## Dependency rules

- Governance precedes automation because controls require owners and review authority.
- Boundaries precede module tooling because the project graph needs stable locations.
- Toolchain pins precede lock files and reproducible checks.
- Local checks precede CI so developers can reproduce failures.
- Security and evidence workflows depend on locked dependency inputs.
- Future module onboarding depends on every P1 capability and remains blocked until separate
  architecture approval.
