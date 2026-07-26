package com.newland.erp.crm.application;

import java.util.UUID;

public interface CrmSecurityPort {
  String currentActor();

  void require(String actor, String capability, UUID companyId);
}
