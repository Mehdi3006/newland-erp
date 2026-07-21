package com.newland.erp.finance.infrastructure;

import com.newland.erp.finance.application.FinancePorts;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

public final class FinanceInfrastructureAdapters {
  @Component
  public static final class EnterpriseAdapter implements FinancePorts.EnterprisePort {
    public void requireCompanyBranch(final UUID company, final UUID branch) {
      if (company == null || branch == null) {
        throw new IllegalArgumentException("Company and branch scope are required.");
      }
    }
  }

  @Component
  public static final class MasterDataAdapter implements FinancePorts.MasterDataPort {
    public void requireCurrency(final UUID currency) {
      if (currency == null) {
        throw new IllegalArgumentException("Currency is required.");
      }
    }
  }

  @Component
  public static final class AuthorizationAdapter implements FinancePorts.AuthorizationPort {
    public void require(final String actor, final String capability, final UUID company) {
      if (actor == null || actor.isBlank() || company == null) {
        throw new IllegalArgumentException("Authorized company scope is required.");
      }
    }

    public void requireCostCenter(final String actor, final UUID id) {
      if (id == null) {
        throw new IllegalArgumentException("Cost center is required.");
      }
    }

    public void requireDimension(final String actor, final String id) {
      if (id == null || id.isBlank()) {
        throw new IllegalArgumentException("Dimension is required.");
      }
    }
  }

  @Component
  public static final class NumberAdapter implements FinancePorts.NumberSeriesPort {
    private final AtomicLong n = new AtomicLong();

    public String next(final String s) {
      return s + "-" + n.incrementAndGet();
    }
  }

  @Component
  public static final class PlatformAdapter
      implements FinancePorts.AuditPort, FinancePorts.OutboxPort, FinancePorts.AttachmentPort {
    public void record(final String a, final String b, final UUID c) {}

    public void publish(final String a, final UUID b) {}

    public void attach(final UUID a, final UUID b) {
      if (b == null) {
        throw new IllegalArgumentException("Attachment is required.");
      }
    }
  }

  private FinanceInfrastructureAdapters() {}
}
