package com.newland.erp.finance.posting.infrastructure;

import com.newland.erp.finance.posting.application.PostingPorts;
import com.newland.erp.finance.posting.domain.AccountingEvent;
import com.newland.erp.finance.posting.domain.PostingRule;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

public final class PostingInfrastructureAdapters {
  @Component
  public static final class CompanyAdapter implements PostingPorts.CompanyValidationPort {
    public void requireCompany(final UUID id) {
      require(id, "company");
    }
  }

  @Component
  public static final class BranchAdapter implements PostingPorts.BranchValidationPort {
    public void requireBranch(final UUID company, final UUID branch) {
      require(company, "company");
      require(branch, "branch");
    }
  }

  @Component
  public static final class CurrencyAdapter implements PostingPorts.CurrencyValidationPort {
    public void requireCurrency(final String code) {
      if (code == null || code.isBlank()) {
        throw new IllegalArgumentException("Currency is required.");
      }
    }
  }

  @Component
  public static final class RateAdapter implements PostingPorts.ExchangeRateValidationPort {
    public void requireRate(final String code, final BigDecimal rate, final LocalDate date) {
      if (rate == null || rate.signum() <= 0) {
        throw new IllegalArgumentException("Exchange rate is required.");
      }
    }
  }

  @Component
  public static final class PeriodAdapter implements PostingPorts.AccountingPeriodPort {
    public void requireOpenPeriod(final UUID company, final LocalDate date) {
      require(company, "company");
      if (date == null) {
        throw new IllegalArgumentException("Accounting date is required.");
      }
    }
  }

  @Component
  public static final class DimensionsAdapter
      implements PostingPorts.FinancialDimensionValidationPort {
    public void requireDimensions(final UUID company, final Map<String, String> dimensions) {
      require(company, "company");
    }
  }

  @Component
  public static final class JournalAdapter implements PostingPorts.JournalPostingPort {
    public PostingPorts.JournalReference createAndPost(
        final AccountingEvent event, final PostingRule rule) {
      return new PostingPorts.JournalReference(UUID.randomUUID(), "POSTING-PENDING");
    }
  }

  @Component
  public static final class PlatformAdapter
      implements PostingPorts.AuditPort, PostingPorts.TransactionalOutboxPort {
    public void record(final String actor, final String event, final UUID id) {}

    public void publish(final String event, final UUID id) {}
  }

  private static void require(final UUID id, final String name) {
    if (id == null) {
      throw new IllegalArgumentException(name + " is required.");
    }
  }

  private PostingInfrastructureAdapters() {}
}
