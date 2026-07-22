package com.newland.erp.finance.application;

import java.util.UUID;

public final class FinancePorts {
  public interface EnterprisePort {
    void requireCompanyBranch(UUID companyId, UUID branchId);
  }

  public interface MasterDataPort {
    void requireCurrency(UUID currencyId);
  }

  public interface AuthorizationPort {
    void require(String actor, String capability, UUID companyId);

    void requireCostCenter(String actor, UUID costCenterId);

    void requireDimension(String actor, String dimensionCode);
  }

  public interface NumberSeriesPort {
    String next(String series);
  }

  public interface AuditPort {
    void record(String actor, String action, UUID id);
  }

  public interface OutboxPort {
    void publish(String type, UUID aggregateId);
  }

  public interface AttachmentPort {
    void attach(UUID aggregateId, UUID attachmentId);
  }

  /** Explicit inbound port for future subledgers; it does not post automatically. */
  public interface FinancePostingPort {
    void requestPosting(UUID sourceDocumentId, UUID companyId);
  }

  private FinancePorts() {}
}
