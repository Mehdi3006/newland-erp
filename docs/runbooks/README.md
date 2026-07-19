# Runbooks

Runbooks are executable procedures. Each procedure identifies prerequisites, validation, failure
handling, and escalation.

- [`local-development.md`](local-development.md) bootstraps and verifies a workstation.
- [`dependency-upgrade.md`](dependency-upgrade.md) updates dependencies safely.
- [`release.md`](release.md) defines the future release process.
- [`security-response.md`](security-response.md) coordinates vulnerability response.

Commands must be tested when the related tooling changes. Runtime operational runbooks will be added
only after runtime architecture is approved.
