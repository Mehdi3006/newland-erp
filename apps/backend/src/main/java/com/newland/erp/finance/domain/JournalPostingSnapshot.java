package com.newland.erp.finance.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Immutable monetary and statutory context captured when a journal is posted. */
public record JournalPostingSnapshot(
    UUID journalEntryId,
    String transactionCurrency,
    String baseCurrency,
    UUID exchangeRateId,
    String exchangeRateSource,
    String exchangeRateType,
    LocalDate exchangeRateDate,
    BigDecimal exchangeRate,
    BigDecimal transactionAmount,
    BigDecimal baseAmount,
    Map<String, String> taxContext) {
  public JournalPostingSnapshot {
    AccountingPeriodContract.required(journalEntryId, "journal entry id");
    transactionCurrency = currency(transactionCurrency, "transaction currency");
    baseCurrency = currency(baseCurrency, "base currency");
    exchangeRateSource =
        AccountingPeriodContract.text(exchangeRateSource, "exchange-rate source");
    exchangeRateType = AccountingPeriodContract.text(exchangeRateType, "exchange-rate type");
    AccountingPeriodContract.required(exchangeRateDate, "exchange-rate date");
    exchangeRate = positive(exchangeRate, "exchange rate", 12);
    transactionAmount = nonNegative(transactionAmount, "transaction amount");
    baseAmount = nonNegative(baseAmount, "base amount");
    taxContext = taxContext == null ? Map.of() : Map.copyOf(taxContext);
    taxContext.forEach(
        (key, value) -> {
          if (key == null || key.isBlank() || value == null || value.isBlank()) {
            throw new FinanceException("Tax snapshot keys and values are required.");
          }
        });
    if (transactionCurrency.equals(baseCurrency) && exchangeRate.compareTo(BigDecimal.ONE) != 0) {
      throw new FinanceException("Base-currency journal exchange rate must equal one.");
    }
    if (!transactionAmount.multiply(exchangeRate).setScale(6, RoundingMode.HALF_UP)
        .equals(baseAmount)) {
      throw new FinanceException("Journal base amount does not match its exchange-rate snapshot.");
    }
  }

  private static String currency(final String value, final String label) {
    final String normalized = AccountingPeriodContract.text(value, label).toUpperCase(Locale.ROOT);
    if (!normalized.matches("[A-Z]{3}")) {
      throw new IllegalArgumentException(label + " must be a three-letter code.");
    }
    return normalized;
  }

  private static BigDecimal positive(
      final BigDecimal value, final String label, final int scale) {
    if (value == null || value.signum() <= 0) {
      throw new FinanceException(label + " must be positive.");
    }
    return value.setScale(scale, RoundingMode.HALF_UP);
  }

  private static BigDecimal nonNegative(final BigDecimal value, final String label) {
    if (value == null || value.signum() < 0) {
      throw new FinanceException(label + " cannot be missing or negative.");
    }
    return value.setScale(6, RoundingMode.HALF_UP);
  }
}
