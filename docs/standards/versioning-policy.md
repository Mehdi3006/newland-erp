# Versioning Policy

Newland ERP uses Semantic Versioning once a distributable is released:

```text
MAJOR.MINOR.PATCH
```

- `MAJOR` introduces an incompatible public contract.
- `MINOR` adds a backward-compatible capability.
- `PATCH` contains backward-compatible fixes or security maintenance.

Before `1.0.0`, incompatible changes are permitted only through an approved ADR and must be
highlighted in the changelog. Pre-releases use identifiers such as `1.0.0-alpha.1` or `1.0.0-rc.1`.

## Release source

Releases are built from an immutable commit on `main` and tagged `vMAJOR.MINOR.PATCH`. Tags must be
annotated and, when repository support is configured, signed. Build artifacts are produced by CI
rather than developer machines.

## Compatibility

Each public API or contract must document its compatibility promise before its first release.
Database, event, and external API compatibility policies require their own architecture decisions.

## Changelog

Every user-visible, operational, security, or incompatible change updates `CHANGELOG.md` under
`Unreleased`. Release automation moves entries to a dated version heading and updates comparison
links.
