package com.newland.erp.finance.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Persistence-neutral published journal view for reconciliation and source-document traceability. */
public record JournalEntryContract(
    UUID journalEntryId,
    String journalNumber,
    String idempotencyKey,
    UUID companyId,
    UUID branchId,
    UUID fiscalYearId,
    UUID accountingPeriodId,
    LocalDate postingDate,
    Status status,
    String sourceDocumentType,
    UUID sourceDocumentId,
    UUID reversalOfJournalId,
    List<Line> lines,
    int version) {
  public JournalEntryContract {
    AccountingPeriodContract.required(journalEntryId, "journal entry id");
    journalNumber = AccountingPeriodContract.text(journalNumber, "journal number");
    idempotencyKey = AccountingPeriodContract.text(idempotencyKey, "idempotency key");
    AccountingPeriodContract.required(companyId, "company id");
    AccountingPeriodContract.required(fiscalYearId, "fiscal year id");
    AccountingPeriodContract.required(accountingPeriodId, "accounting period id");
    AccountingPeriodContract.required(postingDate, "posting date");
    AccountingPeriodContract.required(status, "journal status");
    sourceDocumentType = optionalText(sourceDocumentType);
    if ((sourceDocumentId == null) != sourceDocumentType.isEmpty()) {
      throw new IllegalArgumentException(
          "Journal source document type and identifier must be supplied together.");
    }
    lines = lines == null ? List.of() : List.copyOf(lines);
    validateBalanced(lines);
    if (version < 0) {
      throw new IllegalArgumentException("Journal version cannot be negative.");
    }
    if (status == Status.REVERSED && reversalOfJournalId == null) {
      throw new IllegalArgumentException("Reversed journal requires its reversal reference.");
    }
  }

  private static void validateBalanced(final List<Line> journalLines) {
    if (journalLines.size() < 2) {
      throw new FinanceException("Journal contract requires at least two lines.");
    }
    final BigDecimal debit =
        journalLines.stream().map(Line::debit).reduce(BigDecimal.ZERO, BigDecimal::add);
    final BigDecimal credit =
        journalLines.stream().map(Line::credit).reduce(BigDecimal.ZERO, BigDecimal::add);
    if (debit.compareTo(credit) != 0) {
      throw new FinanceException("Journal contract debit must equal credit.");
    }
  }

  private static String optionalText(final String value) {
    return value == null ? "" : value.trim();
  }

  public enum Status {
    DRAFT,
    POSTED,
    REVERSED
  }

  public record Line(
      UUID lineId,
      UUID accountId,
      BigDecimal debit,
      BigDecimal credit,
      UUID costCenterId,
      UUID profitCenterId,
      String financialDimension,
      String transactionCurrency,
      BigDecimal transactionAmount,
      BigDecimal exchangeRateSnapshot) {
    public Line {
      AccountingPeriodContract.required(lineId, "journal line id");
      AccountingPeriodContract.required(accountId, "account id");
      debit = amount(debit);
      credit = amount(credit);
      if ((debit.signum() == 0 && credit.signum() == 0)
          || (debit.signum() > 0 && credit.signum() > 0)) {
        throw new FinanceException("Journal line requires exactly one debit or credit amount.");
      }
      financialDimension = optionalText(financialDimension);
      transactionCurrency = currency(transactionCurrency);
      if (transactionAmount == null || transactionAmount.signum() < 0) {
        throw new FinanceException("Transaction amount cannot be missing or negative.");
      }
      transactionAmount = transactionAmount.setScale(6, RoundingMode.HALF_UP);
      if (exchangeRateSnapshot == null || exchangeRateSnapshot.signum() <= 0) {
        throw new FinanceException("Exchange-rate snapshot must be positive.");
      }
      exchangeRateSnapshot = exchangeRateSnapshot.setScale(12, RoundingMode.HALF_UP);
    }

    private static BigDecimal amount(final BigDecimal value) {
      if (value == null || value.signum() < 0) {
        throw new FinanceException("Journal debit and credit cannot be missing or negative.");
      }
      return value.setScale(6, RoundingMode.HALF_UP);
    }

    private static String currency(final String value) {
      final String normalized = AccountingPeriodContract.text(value, "transaction currency");
      if (!normalized.matches("[A-Za-z]{3}")) {
        throw new IllegalArgumentException("Currency must be a three-letter code.");
      }
      return normalized.toUpperCase(java.util.Locale.ROOT);
    }
  }
}
