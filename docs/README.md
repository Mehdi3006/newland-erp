# Documentation

Documentation is a versioned part of Newland ERP. It is reviewed and released with the code or
configuration it describes.

## Collections

| Directory                                 | Purpose                                          | Change authority                        |
| ----------------------------------------- | ------------------------------------------------ | --------------------------------------- |
| [`adr/`](adr/README.md)                   | Durable architecture decisions and their context | Architecture owner plus affected owners |
| [`architecture/`](architecture/README.md) | Current system and repository design             | Architecture owner                      |
| [`standards/`](standards/README.md)       | Mandatory engineering and governance rules       | Repository maintainers                  |
| [`runbooks/`](runbooks/README.md)         | Repeatable operational procedures                | Procedure owner                         |
| [`decisions/`](decisions/README.md)       | Time-bounded, non-architectural decision records | Affected owner                          |

An ADR explains why a material architecture decision was made. Architecture documents explain the
current design. Standards state rules. Runbooks describe how to execute a procedure. Decision
records capture smaller choices that do not justify an ADR.

## Documentation rules

- Prefer one canonical document and link to it rather than duplicating content.
- Use repository-relative links.
- State an owner, status, and review date when content can become stale.
- Update documentation in the same pull request as its subject.
- Never include secrets, customer records, or fake business examples.

The complete rules are in [`standards/documentation-policy.md`](standards/documentation-policy.md).
