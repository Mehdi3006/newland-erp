# Coding Standards

## General

- Optimize first for correctness, clarity, and maintainability.
- Keep modules cohesive and expose the smallest practical public API.
- Reject invalid input at the owning boundary.
- Represent errors explicitly; do not silently discard failures.
- Keep nondeterministic behavior behind interfaces so it can be tested.
- Never log secrets, tokens, personal records, or regulated business data.
- Comments explain intent and constraints, not syntax.

## Java and Kotlin

- Use the configured Java toolchain; do not depend on a developer's default JDK.
- Prefer immutable values and constructor-provided dependencies.
- Keep packages aligned to an approved module boundary.
- Run tests on JUnit Platform.
- Add ArchUnit rules when a JVM module introduces enforceable dependency boundaries.
- Apply Checkstyle and Spotless. Kotlin modules additionally apply the approved Detekt convention.

## TypeScript

- Enable strict compiler options.
- Avoid `any`; justify narrow exceptions next to the code.
- Use explicit package entry points rather than importing internal paths.
- Prefer pure functions and immutable data at shared boundaries.
- Keep browser, Node.js, and framework globals isolated by project configuration.

## Testing

Tests must be deterministic, independent, and meaningful at the lowest useful level. A defect fix
includes a failing test where practical. Tests must not call real production services or use
customer data. Fake ERP business data is not permitted in P1.
