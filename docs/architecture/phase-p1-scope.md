# Phase P1 Scope

## Objective

Phase P1 creates the repository foundation required for consistent future development.

## Included

- repository governance and ownership;
- architecture, standards, decisions, and runbooks;
- reproducible Java, Node.js, Gradle, pnpm, Nx, and TypeScript toolchains;
- formatting, linting, unit-test, and architecture-test foundations;
- CI for build quality and supply-chain evidence;
- documented top-level boundaries for future work.

## Explicitly excluded

- Spring Boot or any other server application;
- React or any other user interface;
- databases, migrations, schemas, or persistence configuration;
- entities, controllers, endpoints, or API specifications;
- inventory, product, CRM, finance, HR, or other ERP behavior;
- dashboards, demonstrations, seeded records, or fake business data;
- production infrastructure.

The script `tools/architecture/verify.mjs` fails if common runtime source or database artifacts
appear in reserved P1 directories.

## Exit criteria

P1 is complete when all documented files and boundaries exist, clean installs are reproducible, all
local checks pass, CI workflows are valid, the change is committed and pushed, and further runtime
work pauses for architecture approval.
