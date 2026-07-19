# ArchUnit Placeholder

Future JVM modules receive the ArchUnit JUnit dependency from the root Gradle conventions. Each
approved module must add architecture tests that enforce its accepted dependency rules.

Initial rules should cover:

- libraries do not depend on applications;
- domain code does not depend on delivery or persistence adapters;
- packages do not bypass approved public entry points;
- cyclic module dependencies are prohibited.

No placeholder Java class is created in P1 because there is no approved JVM module or package
boundary to test.
