package com.newland.erp.finance.infrastructure;

import com.newland.erp.finance.application.FinancePorts;
import com.newland.erp.platform.application.integration.PlatformAuditOutboxPort;
import java.util.Map;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
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
    private final DSLContext dsl;

    public NumberAdapter(final DSLContext dslContext) {
      dsl = dslContext;
    }

    public String next(final String s) {
      final Long value =
          dsl.select(DSL.field("nextval('finance_journal_number_seq')", Long.class))
              .fetchOne(0, Long.class);
      return s + "-" + value;
    }
  }

  @Component
  public static final class PlatformAdapter
      implements FinancePorts.AuditPort, FinancePorts.OutboxPort, FinancePorts.AttachmentPort {
    private final PlatformAuditOutboxPort platform;

    public PlatformAdapter(final PlatformAuditOutboxPort platformPort) {
      platform = platformPort;
    }

    public void record(final String actor, final String action, final UUID id) {
      platform.recordAudit(actor, action, "FinanceJournal", id, Map.of());
    }

    public void publish(final String eventType, final UUID id) {
      platform.publishEvent("finance", eventType, id, Map.of());
    }

    public void attach(final UUID aggregateId, final UUID attachmentId) {
      if (attachmentId == null) {
        throw new IllegalArgumentException("Attachment is required.");
      }
      platform.attachFile("finance", "JournalEntry", aggregateId, attachmentId);
    }
  }

  private FinanceInfrastructureAdapters() {}
}
