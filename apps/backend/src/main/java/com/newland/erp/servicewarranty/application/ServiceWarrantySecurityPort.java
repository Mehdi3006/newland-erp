package com.newland.erp.servicewarranty.application;

import java.util.UUID;

public interface ServiceWarrantySecurityPort {
  String currentActor();

  void require(String actor, String capability, UUID companyId);
}
