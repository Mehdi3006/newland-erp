package com.newland.erp.finance.api;

import com.newland.erp.finance.domain.AccountingPeriodContract;
import com.newland.erp.finance.domain.CurrencyExchangeContract;
import com.newland.erp.finance.domain.FinancialDocumentNumber;
import com.newland.erp.finance.domain.JournalEntryContract;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Application DTOs for published Finance foundation contract responses. */
public final class FinanceFoundationContractDtos {
  public record AccountingPeriodResponse(
      UUID companyId,
      UUID fiscalYearId,
      UUID accountingPeriodId,
      String periodCode,
      LocalDate startsOn,
      LocalDate endsOn,
      String state) {
    public static AccountingPeriodResponse from(final AccountingPeriodContract value) {
      return new AccountingPeriodResponse(
          value.companyId(),
          value.fiscalYearId(),
          value.accountingPeriodId(),
          value.periodCode(),
          value.startsOn(),
          value.endsOn(),
          value.state().name());
    }
  }

  public record JournalResponse(
      UUID journalEntryId,
      String journalNumber,
      UUID companyId,
      LocalDate postingDate,
      String status,
      String sourceDocumentType,
      UUID sourceDocumentId,
      List<JournalLineResponse> lines,
      int version) {
    public static JournalResponse from(final JournalEntryContract value) {
      return new JournalResponse(
          value.journalEntryId(),
          value.journalNumber(),
          value.companyId(),
          value.postingDate(),
          value.status().name(),
          value.sourceDocumentType(),
          value.sourceDocumentId(),
          value.lines().stream().map(JournalLineResponse::from).toList(),
          value.version());
    }
  }

  public record JournalLineResponse(
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
    static JournalLineResponse from(final JournalEntryContract.Line value) {
      return new JournalLineResponse(
          value.lineId(),
          value.accountId(),
          value.debit(),
          value.credit(),
          value.costCenterId(),
          value.profitCenterId(),
          value.financialDimension(),
          value.transactionCurrency(),
          value.transactionAmount(),
          value.exchangeRateSnapshot());
    }
  }

  public record DocumentNumberResponse(
      String number,
      String documentType,
      UUID companyId,
      UUID fiscalYearId,
      UUID documentId,
      Instant assignedAt) {
    public static DocumentNumberResponse from(final FinancialDocumentNumber.Assignment value) {
      return new DocumentNumberResponse(
          value.number(),
          value.documentType(),
          value.companyId(),
          value.fiscalYearId(),
          value.documentId(),
          value.assignedAt());
    }
  }

  public record ExchangeRateResponse(
      UUID rateId,
      UUID companyId,
      String sourceCurrency,
      String targetCurrency,
      String rateType,
      String source,
      LocalDate validFrom,
      LocalDate validTo,
      BigDecimal rate) {
    public static ExchangeRateResponse from(final CurrencyExchangeContract.RateSnapshot value) {
      return new ExchangeRateResponse(
          value.rateId(),
          value.companyId(),
          value.sourceCurrency(),
          value.targetCurrency(),
          value.rateType(),
          value.source(),
          value.validFrom(),
          value.validTo(),
          value.rate());
    }
  }

  private FinanceFoundationContractDtos() {}
}
