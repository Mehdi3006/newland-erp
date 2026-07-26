package com.newland.erp.masterdata.application.integration;

public interface MasterDataReferencePort {
  boolean isActiveCurrency(String currencyCode);

  default boolean isActiveReference(final String referenceType, final String referenceCode) {
    return false;
  }

  java.util.Optional<ExchangeRateSnapshot> resolveExchangeRate(
      java.util.UUID companyId, String sourceCurrency, String targetCurrency,
      java.time.LocalDate effectiveDate);

  record ExchangeRateSnapshot(java.util.UUID rateId, java.util.UUID companyId,
                              String sourceCurrency, String targetCurrency,
                              java.time.LocalDate validFrom, java.time.LocalDate validTo,
                              java.math.BigDecimal rate) {}
}
