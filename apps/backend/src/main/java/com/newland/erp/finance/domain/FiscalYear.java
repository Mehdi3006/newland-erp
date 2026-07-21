package com.newland.erp.finance.domain;

import java.time.LocalDate;
import java.util.UUID;

public record FiscalYear(
    UUID id, UUID companyId, String code, LocalDate startsOn, LocalDate endsOn, boolean closed) {
  public FiscalYear {
    if (id == null
        || companyId == null
        || code == null
        || code.isBlank()
        || startsOn == null
        || endsOn == null
        || endsOn.isBefore(startsOn)) {
      throw new IllegalArgumentException("Fiscal year boundaries are invalid.");
    }
  }

  public boolean contains(final LocalDate date) {
    return !date.isBefore(startsOn) && !date.isAfter(endsOn);
  }
}
