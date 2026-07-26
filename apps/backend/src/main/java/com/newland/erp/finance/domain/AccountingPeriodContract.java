package com.newland.erp.finance.domain;

import java.time.LocalDate;
import java.util.UUID;

/** Immutable published view of the period that controls a proposed Finance posting. */
public record AccountingPeriodContract(
    UUID companyId,
    UUID fiscalYearId,
    UUID accountingPeriodId,
    String periodCode,
    LocalDate startsOn,
    LocalDate endsOn,
    State state) {
  public AccountingPeriodContract {
    required(companyId, "company id");
    required(fiscalYearId, "fiscal year id");
    required(accountingPeriodId, "accounting period id");
    periodCode = text(periodCode, "period code");
    required(startsOn, "period start");
    required(endsOn, "period end");
    required(state, "period state");
    if (endsOn.isBefore(startsOn)) {
      throw new IllegalArgumentException("Accounting period boundaries are invalid.");
    }
  }

  public void requirePostingAllowed(final LocalDate postingDate, final PostingPurpose purpose) {
    required(postingDate, "posting date");
    required(purpose, "posting purpose");
    if (postingDate.isBefore(startsOn) || postingDate.isAfter(endsOn)) {
      throw new FinanceException("Posting date is outside the accounting period.");
    }
    if (state == State.CLOSED
        || (state == State.CLOSING && purpose != PostingPurpose.CLOSE_ADJUSTMENT)) {
      throw new FinanceException("Accounting period does not allow this posting purpose.");
    }
  }

  public enum State {
    OPEN,
    CLOSING,
    CLOSED
  }

  public enum PostingPurpose {
    ORDINARY,
    CLOSE_ADJUSTMENT
  }

  static String text(final String value, final String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " is required.");
    }
    return value.trim();
  }

  static void required(final Object value, final String name) {
    if (value == null) {
      throw new IllegalArgumentException(name + " is required.");
    }
  }
}
