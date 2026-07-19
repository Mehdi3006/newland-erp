# Commit Conventions

Newland ERP uses Conventional Commits:

```text
<type>(optional-scope): <imperative summary>

optional body

optional footer
```

Allowed types are:

- `feat`: a user-visible or platform capability;
- `fix`: a defect correction;
- `docs`: documentation only;
- `refactor`: behavior-preserving restructuring;
- `test`: test-only changes;
- `build`: build system or dependency changes;
- `ci`: continuous-integration changes;
- `perf`: measurable performance improvement;
- `chore`: maintenance not covered above;
- `revert`: an explicit reversal.

Use `!` and a `BREAKING CHANGE:` footer for incompatible changes. Keep the summary under 72
characters, imperative, and without a period. The body explains why and calls out migration or risk.
Reference issues or ADRs in footers.

Examples:

```text
build: establish repository foundation
docs(adr): record service boundary decision
feat(auth)!: replace token claim contract
```

Commit history must not contain secrets, generated noise, or unrelated changes.
