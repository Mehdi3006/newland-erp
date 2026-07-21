package com.newland.erp.finance.domain;

import java.time.LocalDate;
import java.util.UUID;

public record AccountingPeriod(
    UUID id, UUID fiscalYearId, String code, LocalDate startsOn, LocalDate endsOn, boolean closed) {
  public AccountingPeriod {
    if (id == null
        || fiscalYearId == null
        || code == null
        || code.isBlank()
        || startsOn == null
        || endsOn == null
        || endsOn.isBefore(startsOn)) {
      throw new IllegalArgumentException("Accounting period boundaries are invalid.");
    }
  }

  public boolean contains(final LocalDate date) {
    return !date.isBefore(startsOn) && !date.isAfter(endsOn);
  }
}
