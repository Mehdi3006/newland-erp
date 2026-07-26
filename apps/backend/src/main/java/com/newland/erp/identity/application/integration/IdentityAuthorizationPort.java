package com.newland.erp.identity.application.integration;

import java.util.UUID;

public interface IdentityAuthorizationPort {
  boolean isCompanyCapabilityGranted(UUID userId, String capability, UUID companyId);

  boolean isSystemEnterpriseCapabilityGranted(UUID userId, String capability);
}
