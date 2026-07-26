package com.newland.erp.logistics.infrastructure;

import com.newland.erp.logistics.application.LogisticsMasterReferencePort;
import com.newland.erp.masterdata.application.integration.MasterDataReferencePort;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

@Component
public final class JooqLogisticsMasterReferenceAdapter
    implements LogisticsMasterReferencePort {
  private final DSLContext dsl;
  private final MasterDataReferencePort masterData;

  public JooqLogisticsMasterReferenceAdapter(
      final DSLContext dslContext, final MasterDataReferencePort masterDataReferencePort) {
    dsl = dslContext;
    masterData = masterDataReferencePort;
  }

  @Override
  public void requireActiveCarrier(final String carrierCode) {
    requireActive("logistics_carrier", carrierCode, "carrier");
  }

  @Override
  public void requireActivePort(final String portCode) {
    requireActive("logistics_port", portCode, "port");
  }

  @Override
  public void requireActiveIncoterm(final String incotermCode) {
    if (!masterData.isActiveReference("incoterms", incotermCode)) {
      throw new IllegalArgumentException("Active Incoterm reference not found.");
    }
  }

  private void requireActive(
      final String tableName, final String referenceCode, final String referenceName) {
    if (referenceCode == null
        || referenceCode.isBlank()
        || !dsl.fetchExists(
            DSL.table(tableName),
            DSL.field("code", String.class).eq(referenceCode.trim().toUpperCase())
                .and(DSL.field("active", Boolean.class).eq(true)))) {
      throw new IllegalArgumentException(
          "Active logistics " + referenceName + " reference not found.");
    }
  }
}
