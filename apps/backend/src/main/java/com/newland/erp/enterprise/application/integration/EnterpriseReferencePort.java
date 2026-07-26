package com.newland.erp.enterprise.application.integration;

import java.util.UUID;

public interface EnterpriseReferencePort {
  boolean isActiveCompany(UUID companyId);

  boolean isActiveBranch(UUID companyId, UUID branchId);

  java.util.Optional<String> companyBaseCurrency(UUID companyId);
}
