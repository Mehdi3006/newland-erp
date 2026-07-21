package com.newland.erp.finance.domain;

import java.util.UUID;

public record CostCenter(UUID id, UUID companyId, String code, boolean active) {
  public CostCenter {
    if (id == null || companyId == null || code == null || code.isBlank()) {
      throw new IllegalArgumentException("Cost center is required.");
    }
  }
}
