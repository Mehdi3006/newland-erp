package com.newland.erp.finance.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record JournalEntry(
    UUID id,
    String number,
    String idempotencyKey,
    UUID companyId,
    UUID branchId,
    UUID fiscalYearId,
    UUID periodId,
    LocalDate postingDate,
    JournalStatus status,
    List<JournalLine> lines,
    UUID reversalOfId,
    int lockVersion,
    Instant createdAt,
    String actor) {
  public JournalEntry {
    if (id == null
        || companyId == null
        || fiscalYearId == null
        || periodId == null
        || postingDate == null
        || status == null
        || createdAt == null
        || number == null
        || number.isBlank()
        || idempotencyKey == null
        || idempotencyKey.isBlank()
        || actor == null
        || actor.isBlank()) {
      throw new IllegalArgumentException("Journal identity and posting metadata are required.");
    }
    lines = lines == null ? List.of() : List.copyOf(lines);
    validateLines(lines);
  }

  public JournalEntry post() {
    if (status != JournalStatus.DRAFT) {
      throw new FinanceException("Only draft journals can be posted.");
    }
    return new JournalEntry(
        id,
        number,
        idempotencyKey,
        companyId,
        branchId,
        fiscalYearId,
        periodId,
        postingDate,
        JournalStatus.POSTED,
        lines,
        reversalOfId,
        lockVersion,
        createdAt,
        actor);
  }

  public JournalEntry revise(final List<JournalLine> revisedLines) {
    if (status != JournalStatus.DRAFT) {
      throw new FinanceException("Posted journals are immutable.");
    }
    return new JournalEntry(
        id,
        number,
        idempotencyKey,
        companyId,
        branchId,
        fiscalYearId,
        periodId,
        postingDate,
        status,
        revisedLines,
        reversalOfId,
        lockVersion,
        createdAt,
        actor);
  }

  public JournalEntry withLockVersion(final int version) {
    if (version < 0) {
      throw new IllegalArgumentException("Journal lock version cannot be negative.");
    }
    return new JournalEntry(
        id,
        number,
        idempotencyKey,
        companyId,
        branchId,
        fiscalYearId,
        periodId,
        postingDate,
        status,
        lines,
        reversalOfId,
        version,
        createdAt,
        actor);
  }

  public static void validateLines(final List<JournalLine> entries) {
    if (entries.size() < 2) {
      throw new FinanceException("Journal entries require at least two lines.");
    }
    final BigDecimal debit =
        entries.stream().map(JournalLine::debit).reduce(BigDecimal.ZERO, BigDecimal::add);
    final BigDecimal credit =
        entries.stream().map(JournalLine::credit).reduce(BigDecimal.ZERO, BigDecimal::add);
    if (debit.compareTo(credit) != 0) {
      throw new FinanceException("Total debit must equal total credit.");
    }
  }

  public enum JournalStatus {
    DRAFT,
    POSTED,
    REVERSED
  }

  public record JournalLine(
      UUID id,
      UUID accountId,
      BigDecimal debit,
      BigDecimal credit,
      UUID costCenterId,
      UUID profitCenterId,
      String dimensionCode,
      UUID currencyId,
      BigDecimal currencyAmount,
      BigDecimal exchangeRateSnapshot) {
    public JournalLine {
      if (id == null || accountId == null) {
        throw new IllegalArgumentException("Journal line and account are required.");
      }
      debit = value(debit);
      credit = value(credit);
      if (debit.signum() < 0 || credit.signum() < 0) {
        throw new FinanceException("Journal debit and credit amounts cannot be negative.");
      }
      if ((debit.signum() == 0 && credit.signum() == 0)
          || (debit.signum() > 0 && credit.signum() > 0)) {
        throw new FinanceException(
            "Journal line must have exactly one non-zero debit or credit amount.");
      }
      if (currencyAmount != null && currencyAmount.scale() > 6) {
        currencyAmount = currencyAmount.setScale(6, RoundingMode.HALF_UP);
      }
      if (exchangeRateSnapshot != null && exchangeRateSnapshot.signum() <= 0) {
        throw new FinanceException("Exchange rate snapshot must be positive.");
      }
    }

    private static BigDecimal value(final BigDecimal amount) {
      return amount == null ? BigDecimal.ZERO : amount.setScale(6, RoundingMode.HALF_UP);
    }
  }
}
