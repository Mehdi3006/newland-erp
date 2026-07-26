package com.newland.erp.finance.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

/** Company-scoped currency and effective-rate contract used before financial posting. */
public final class CurrencyExchangeContract {
  public record Currency(String code, int fractionDigits, boolean active) {
    public Currency {
      code = currencyCode(code);
      if (fractionDigits < 0 || fractionDigits > 6) {
        throw new IllegalArgumentException("Currency fraction digits must be between zero and six.");
      }
    }
  }

  public record RateQuery(
      UUID companyId,
      String sourceCurrency,
      String targetCurrency,
      String rateType,
      LocalDate effectiveDate) {
    public RateQuery {
      AccountingPeriodContract.required(companyId, "company id");
      sourceCurrency = currencyCode(sourceCurrency);
      targetCurrency = currencyCode(targetCurrency);
      rateType = AccountingPeriodContract.text(rateType, "rate type").toUpperCase(Locale.ROOT);
      AccountingPeriodContract.required(effectiveDate, "exchange-rate effective date");
    }
  }

  public record RateSnapshot(
      UUID rateId,
      UUID companyId,
      String sourceCurrency,
      String targetCurrency,
      String rateType,
      String source,
      LocalDate validFrom,
      LocalDate validTo,
      BigDecimal rate) {
    public RateSnapshot {
      AccountingPeriodContract.required(rateId, "rate id");
      AccountingPeriodContract.required(companyId, "company id");
      sourceCurrency = currencyCode(sourceCurrency);
      targetCurrency = currencyCode(targetCurrency);
      rateType = AccountingPeriodContract.text(rateType, "rate type").toUpperCase(Locale.ROOT);
      source = AccountingPeriodContract.text(source, "exchange-rate source");
      AccountingPeriodContract.required(validFrom, "rate validity start");
      if (validTo != null && validTo.isBefore(validFrom)) {
        throw new IllegalArgumentException("Exchange-rate validity is invalid.");
      }
      if (rate == null || rate.signum() <= 0) {
        throw new FinanceException("Exchange rate must be positive.");
      }
      rate = rate.setScale(12, RoundingMode.HALF_UP);
    }

    public void requireMatches(final RateQuery query) {
      AccountingPeriodContract.required(query, "rate query");
      if (!companyId.equals(query.companyId())
          || !sourceCurrency.equals(query.sourceCurrency())
          || !targetCurrency.equals(query.targetCurrency())
          || !rateType.equals(query.rateType())
          || query.effectiveDate().isBefore(validFrom)
          || (validTo != null && query.effectiveDate().isAfter(validTo))) {
        throw new FinanceException("Exchange-rate snapshot does not satisfy the requested scope.");
      }
    }
  }

  private static String currencyCode(final String value) {
    final String normalized = AccountingPeriodContract.text(value, "currency code");
    if (!normalized.matches("[A-Za-z]{3}")) {
      throw new IllegalArgumentException("Currency must be a three-letter code.");
    }
    return normalized.toUpperCase(Locale.ROOT);
  }

  private CurrencyExchangeContract() {}
}
