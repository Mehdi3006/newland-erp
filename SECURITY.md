# Security Policy

## Supported versions

Newland ERP has not released a production version. Security fixes apply to the latest commit on
`main` and to any explicitly supported release branch listed in the versioning policy.

## Reporting a vulnerability

Do not open a public issue for suspected vulnerabilities, leaked credentials, or exploitable
configuration.

Use GitHub's private vulnerability reporting feature for this repository. If that feature is
unavailable, contact the repository owner privately through their verified GitHub profile. Include:

- affected commit or release;
- reproduction steps and required conditions;
- impact and likely attack path;
- any proposed mitigation;
- whether the issue is already public.

Do not include real customer, credential, financial, or regulated data.

The maintainers aim to acknowledge a report within two business days, provide an initial assessment
within five business days, and coordinate disclosure after remediation. These targets may change
with severity and complexity.

## Repository security controls

Pull requests run dependency, secret, license, SBOM, and architecture checks. Contributors must
never commit secrets. Rotate a credential immediately if it enters Git history; deleting the file is
not sufficient.

See [`docs/runbooks/security-response.md`](docs/runbooks/security-response.md) for the maintainer
response procedure.
