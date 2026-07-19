# GitHub Labels

Labels are documented here so repository configuration can be applied consistently. Colors are
six-character hexadecimal values without `#`.

## Type

| Label                | Color    | Meaning                           |
| -------------------- | -------- | --------------------------------- |
| `type:bug`           | `d73a4a` | Confirmed defect                  |
| `type:feature`       | `0e8a16` | Approved capability               |
| `type:architecture`  | `5319e7` | Architecture decision or boundary |
| `type:documentation` | `0075ca` | Documentation-only work           |
| `type:security`      | `b60205` | Security-sensitive work           |
| `type:maintenance`   | `c5def5` | Tooling or repository maintenance |

## Area

| Label               | Color    | Meaning                     |
| ------------------- | -------- | --------------------------- |
| `area:repository`   | `1d76db` | Root governance and tooling |
| `area:ci`           | `0366d6` | Continuous integration      |
| `area:dependencies` | `0052cc` | Dependency lifecycle        |
| `area:docs`         | `0e8a16` | Governed documentation      |
| `area:platform`     | `006b75` | Platform boundary           |

## Priority and status

| Label                 | Color    | Meaning                                  |
| --------------------- | -------- | ---------------------------------------- |
| `priority:critical`   | `b60205` | Immediate risk or outage                 |
| `priority:high`       | `d93f0b` | Schedule before normal work              |
| `priority:normal`     | `fbca04` | Normal planning                          |
| `status:blocked`      | `000000` | External decision or dependency required |
| `status:needs-design` | `c2e0c6` | Architecture is not approved             |
| `status:ready`        | `0e8a16` | Ready for implementation                 |
| `status:needs-review` | `ededed` | Awaiting owner review                    |

## Automation

Label creation and synchronization are administrative tasks and are not performed by repository code
in P1. The repository owner should apply this catalog in GitHub settings. Labels are additive;
renaming or deleting a label requires checking open issues and automation first.
