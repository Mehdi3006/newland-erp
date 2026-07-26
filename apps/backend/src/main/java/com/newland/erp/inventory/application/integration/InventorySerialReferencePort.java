package com.newland.erp.inventory.application.integration;

import java.util.UUID;

public interface InventorySerialReferencePort {
  void requireSerial(UUID skuId, String serialCode);
}
