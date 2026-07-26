package com.newland.erp.inventory.infrastructure;

import com.newland.erp.inventory.application.integration.InventorySerialReferencePort;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

@Component
public final class InventorySerialReferenceAdapter implements InventorySerialReferencePort {
  private final DSLContext dsl;

  public InventorySerialReferenceAdapter(final DSLContext dslContext) {
    dsl = dslContext;
  }

  @Override
  public void requireSerial(final UUID skuId, final String serialCode) {
    if (serialCode == null
        || serialCode.isBlank()
        || !dsl.fetchExists(
            DSL.table("inventory_serial_number"),
            DSL.field("sku_id", UUID.class).eq(skuId)
                .and(
                    DSL.field("serial_code", String.class)
                        .eq(serialCode.trim().toUpperCase())))) {
      throw new IllegalArgumentException("Inventory serial reference not found.");
    }
  }
}
