package com.newland.erp.finance.domain;

import java.util.UUID;

public record ProfitCenter(UUID id, UUID companyId, String code, boolean active) {
  public ProfitCenter {
    if (id == null || companyId == null || code == null || code.isBlank()) {
      throw new IllegalArgumentException("Profit center is required.");
    }
  }
}
