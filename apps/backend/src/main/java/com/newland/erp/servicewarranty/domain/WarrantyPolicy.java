package com.newland.erp.servicewarranty.domain;

import java.time.LocalDate;
import java.util.UUID;

public record WarrantyPolicy(
    UUID id,
    UUID companyId,
    UUID productId,
    int durationDays,
    boolean serialRequired,
    boolean salesEvidenceRequired,
    LocalDate effectiveFrom,
    LocalDate effectiveTo,
    boolean active) {
  public WarrantyPolicy {
    required(id, "policy id");
    required(companyId, "company id");
    required(effectiveFrom, "effective from");
    if (durationDays <= 0
        || (effectiveTo != null && effectiveTo.isBefore(effectiveFrom))) {
      throw new IllegalArgumentException("Warranty policy duration or effective period is invalid.");
    }
  }

  public boolean applies(final UUID candidateProductId, final LocalDate onDate) {
    return active
        && (productId == null || productId.equals(candidateProductId))
        && !onDate.isBefore(effectiveFrom)
        && (effectiveTo == null || !onDate.isAfter(effectiveTo));
  }

  static void required(final Object value, final String name) {
    if (value == null) {
      throw new IllegalArgumentException(name + " is required.");
    }
  }
}
