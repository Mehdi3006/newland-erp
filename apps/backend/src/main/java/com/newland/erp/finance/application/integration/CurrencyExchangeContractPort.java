package com.newland.erp.finance.application.integration;

import com.newland.erp.finance.domain.CurrencyExchangeContract;

/** Published authoritative currency and exchange-rate resolution boundary. */
public interface CurrencyExchangeContractPort {
  CurrencyExchangeContract.Currency requireActiveCurrency(String currencyCode);

  CurrencyExchangeContract.RateSnapshot requireRate(
      CurrencyExchangeContract.RateQuery rateQuery);
}
