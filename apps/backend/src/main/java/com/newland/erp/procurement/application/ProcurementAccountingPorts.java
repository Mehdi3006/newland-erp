package com.newland.erp.procurement.application;

import java.util.UUID;

public final class ProcurementAccountingPorts {
  public interface SecurityPort {
    String currentActor();

    void requireCompanyCapability(String actor, String capability, UUID companyId);
  }

  private ProcurementAccountingPorts() {}
}
