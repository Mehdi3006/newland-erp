# Repository Layout

## Boundary model

```text
repository
├── governance (.github and root policies)
├── documentation (docs)
├── build foundations (gradle, config, root tool files)
├── future product boundaries
│   ├── apps
│   ├── libs
│   └── contracts
├── future delivery boundary (platform)
└── repository automation (tools)
```

## Top-level responsibilities

| Path         | Allowed responsibility                       | P1 contents                           |
| ------------ | -------------------------------------------- | ------------------------------------- |
| `.github/`   | Collaboration policy and CI workflows        | Templates, ownership, workflows       |
| `apps/`      | Independently deployable runtime units       | Boundary documentation only           |
| `config/`    | Shared tool configuration                    | Checkstyle, Detekt, ArchUnit guidance |
| `contracts/` | Versioned cross-boundary schemas             | Boundary documentation only           |
| `docs/`      | Architecture, standards, runbooks, decisions | Governed Markdown                     |
| `gradle/`    | Gradle wrapper and version catalog           | Build foundation                      |
| `libs/`      | Reusable, non-deployable libraries           | Boundary documentation only           |
| `platform/`  | Delivery and runtime-platform assets         | Boundary documentation only           |
| `tools/`     | Repository automation and verification       | Foundation scripts and tests          |

## Dependency direction

Future applications may depend on approved libraries and contracts. Libraries must not depend on
applications. Contracts must remain independent of application implementations. Platform automation
may consume build outputs but must not contain domain behavior. Repository tooling may inspect all
boundaries but must not become a runtime dependency.

These rules are placeholders until the first module ADR defines concrete tags and enforceable Nx or
ArchUnit constraints.

## Naming

Every future module directory uses kebab-case and has a single clear owner. Deployables and reusable
libraries require their own build descriptor and README. Cross-boundary imports must use published
project entry points rather than relative traversal into implementation directories.
