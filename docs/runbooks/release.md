# Release Runbook

- Owner: Repository owner
- Status: Foundation placeholder
- Last reviewed: 2026-07-19

No runtime artifact is releasable in Phase P1. This runbook defines repository governance for a
future approved release.

## Preconditions

- The release scope and version are approved.
- `main` is green and contains no unresolved critical vulnerability.
- `CHANGELOG.md` is complete.
- Required licenses and SBOMs are generated and retained.
- Rollback and migration procedures are approved for every deployable.

## Procedure

1. Create a release pull request that moves `Unreleased` entries under the version and UTC release
   date.
2. Run all required workflows from the exact release commit.
3. Merge with required owner approvals.
4. Create an annotated `vMAJOR.MINOR.PATCH` tag on that commit.
5. Let CI build and attest artifacts.
6. Publish release notes and artifact checksums.
7. Verify tag, artifacts, SBOM, provenance, and release visibility.

## Failure handling

Do not move an existing tag. Correct code through a new patch release. If artifacts are incomplete
or unverifiable, stop publication and preserve logs. Security-sensitive rollback follows the
security response runbook.

Release automation is intentionally deferred until the first deployable is approved.
