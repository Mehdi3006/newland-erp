# Known Issues

## Business Open Issues

- Legal entity, company, branch, warehouse, project, cost center, and reporting hierarchy require
  confirmation from Newland leadership.
- Country-specific statutory, tax, payroll, invoice numbering, and reporting rules are not
  documented.
- Inventory valuation method is not selected.
- Negative stock policy is not selected.
- Warranty policy is not defined.
- Approval amount limits are not defined.
- Bank, payment gateway, check, and cashbox country rules are not defined.
- Integration priorities are not defined.
- AI assistant data access and approval limits are not defined.

## Technical Open Issues

- P3.1 Enterprise Structure is under PR review and is not approved until explicitly accepted.
- P3.2 Identity and Access has not started.
- Production authentication, user identity source, and permission assignment remain open for P3.2.
- Event transport remains in-process only for P3.1; durable outbox/integration transport is
  deferred.
- List endpoints do not yet provide enterprise-grade pagination/filtering contracts.

## Resolved in This Phase

- Business capability map established.
- Bounded-context inventory established.
- Context ownership and event language established.
- P3.1 recommended scope identified.
- P2 Business Architecture approved.
- P3.1 Enterprise Structure foundation implemented for review in PR #11.
