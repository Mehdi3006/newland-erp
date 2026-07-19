# Foundation Risk Analysis

| Risk                                                       | Likelihood | Impact | Mitigation                                                                           | Trigger for review                                      |
| ---------------------------------------------------------- | ---------- | ------ | ------------------------------------------------------------------------------------ | ------------------------------------------------------- |
| Monorepo boundaries erode as teams grow                    | Medium     | High   | ADRs, CODEOWNERS, Nx tags, ArchUnit expectations, and architecture verification      | First approved runtime module or a cross-boundary cycle |
| Two build ecosystems create duplicated responsibility      | Medium     | Medium | Gradle owns JVM work; pnpm owns installation; Nx owns orchestration                  | A task is implemented in more than one tool             |
| Toolchain upgrades become disruptive                       | Medium     | Medium | Exact versions, wrappers, lock files, Dependabot, and focused upgrade runbook        | Major Node, Java, Gradle, Nx, or TypeScript release     |
| CI action supply chain is compromised                      | Low        | High   | Immutable action SHAs, minimal permissions, and scheduled update review              | Action owner, SHA, or permission change                 |
| Vulnerability scans produce false confidence               | Medium     | High   | OSV plus package audit, secret scanning, SBOMs, and human dependency review          | Unsupported manifest or scanner failure                 |
| Private-repository features are unavailable                | Medium     | Medium | Use portable OSV and Gitleaks controls; document optional GitHub features            | Workflow reports a licensing or feature requirement     |
| Java toolchain download is blocked                         | Medium     | Medium | Support local Java 25 and Foojay provisioning; document both paths                   | Clean CI or workstation cannot resolve a JDK            |
| Excessive quality gates slow feedback                      | Medium     | Medium | Parallel CI jobs, Nx caching, Gradle caching, and affected execution as modules grow | Median PR feedback exceeds ten minutes                  |
| P1 placeholder controls are mistaken for runtime readiness | Medium     | High   | Explicit scope documents, reserved-directory check, and no runtime artifacts         | Request to deploy or add business code without an ADR   |
| Single-person ownership creates continuity risk            | High       | Medium | Document ownership and replace fallback with teams as soon as they exist             | Second maintainer or first product team joins           |

## Residual risk

P1 cannot validate production runtime concerns because no runtime exists. Threat models, data
governance, tenancy, authentication, observability, resilience, deployment, and disaster recovery
remain deliberately unaddressed until the relevant architecture is approved.
