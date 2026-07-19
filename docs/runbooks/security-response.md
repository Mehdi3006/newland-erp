# Security Response Runbook

- Owner: Repository owner
- Visibility: Public procedure; vulnerability details remain private
- Last reviewed: 2026-07-19

## Triage

1. Acknowledge the private report.
2. Restrict details to the smallest response group.
3. Preserve evidence without copying credentials or customer data.
4. Classify affected versions, exploitability, impact, and active exploitation.
5. Assign an incident owner and communication owner.

## Contain

Rotate exposed credentials immediately. Disable compromised automation or releases when necessary.
Use private forks or GitHub security advisories for embargoed fixes. Do not discuss unpatched
exploit details in public issues.

## Remediate

Create the smallest safe correction, add regression coverage, scan transitive dependencies, and
obtain security-sensitive review. Build release artifacts from a protected commit and regenerate
SBOM and license evidence.

## Disclose and recover

Coordinate publication with the reporter when practical. State affected versions, impact,
remediation, and upgrade guidance without exposing unnecessary exploit detail. Monitor for
recurrence, close temporary access, and hold a blameless retrospective.

## Secret in Git history

Revocation is the first action. Removing or rewriting the file does not make the secret safe.
History rewriting requires repository-owner approval, coordination with all consumers, and a
documented recovery plan.
