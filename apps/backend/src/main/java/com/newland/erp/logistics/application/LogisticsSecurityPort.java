package com.newland.erp.logistics.application;

import java.util.UUID;

public interface LogisticsSecurityPort {
  String currentActor();

  void require(String actor, String capability, UUID companyId);
}
