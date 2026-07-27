package com.newland.erp.finance.domain;

import java.time.LocalDate;
import java.util.UUID;

public record AccountingPeriod(
    UUID id, UUID fiscalYearId, String code, LocalDate startsOn, LocalDate endsOn, State state) {
  public AccountingPeriod {
    if (id == null
        || fiscalYearId == null
        || code == null
        || code.isBlank()
        || startsOn == null
        || endsOn == null
        || state == null
        || endsOn.isBefore(startsOn)) {
      throw new IllegalArgumentException("Accounting period boundaries are invalid.");
    }
  }

  public AccountingPeriod(
      final UUID periodId,
      final UUID yearId,
      final String periodCode,
      final LocalDate periodStart,
      final LocalDate periodEnd,
      final boolean isClosed) {
    this(
        periodId,
        yearId,
        periodCode,
        periodStart,
        periodEnd,
        isClosed ? State.CLOSED : State.OPEN);
  }

  public boolean contains(final LocalDate date) {
    return !date.isBefore(startsOn) && !date.isAfter(endsOn);
  }

  public boolean closed() {
    return state == State.CLOSED;
  }

  public void requirePostingAllowed(
      final LocalDate postingDate, final AccountingPeriodContract.PostingPurpose purpose) {
    if (postingDate == null || purpose == null || !contains(postingDate)) {
      throw new FinanceException("Posting date is outside the accounting period.");
    }
    if (state == State.CLOSED
        || (state == State.CLOSING
            && purpose != AccountingPeriodContract.PostingPurpose.CLOSE_ADJUSTMENT)) {
      throw new FinanceException("Accounting period does not allow this posting purpose.");
    }
  }

  public AccountingPeriod transitionTo(final State target) {
    if (target == null || target == state) {
      throw new FinanceException("Accounting period transition must change state.");
    }
    final boolean allowed =
        (state == State.OPEN && target == State.CLOSING)
            || (state == State.CLOSING && (target == State.CLOSED || target == State.OPEN))
            || (state == State.CLOSED && target == State.CLOSING);
    if (!allowed) {
      throw new FinanceException("Invalid accounting period state transition.");
    }
    return new AccountingPeriod(id, fiscalYearId, code, startsOn, endsOn, target);
  }

  public enum State {
    OPEN,
    CLOSING,
    CLOSED
  }
}
