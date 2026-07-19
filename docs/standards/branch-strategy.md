# Branch Strategy

Newland ERP uses short-lived branches from `main`.

## Branch types

| Prefix    | Use                                        |
| --------- | ------------------------------------------ |
| `feat/`   | Approved capability                        |
| `fix/`    | Defect correction                          |
| `docs/`   | Documentation-only change                  |
| `build/`  | Build or dependency change                 |
| `ci/`     | CI-only change                             |
| `chore/`  | Repository maintenance                     |
| `hotfix/` | Urgent correction from a supported release |
| `agent/`  | Automation-authored reviewed change        |

Names use kebab-case after the prefix and should include an issue number when one exists.

## Main branch

`main` is the single integration branch and must remain releasable. Enable:

- pull-request review before merge;
- required status checks;
- conversation resolution;
- protection against force pushes and deletion;
- linear history;
- signed commits when organization support is available.

## Merge policy

Squash merge is the default. The pull request title becomes the Conventional Commit subject. Rebase
merge may be used for an intentionally curated series. Merge commits are reserved for exceptional
release reconciliation.

Delete merged branches. Long-lived release branches exist only for an explicitly supported
maintenance line.
