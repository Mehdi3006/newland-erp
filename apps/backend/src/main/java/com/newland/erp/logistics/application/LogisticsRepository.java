package com.newland.erp.logistics.application;

import com.newland.erp.logistics.domain.LandedCostDraft;
import com.newland.erp.logistics.domain.Shipment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LogisticsRepository {
  boolean idempotencyKeyExists(String idempotencyKey);

  boolean shipmentNumberExists(String shipmentNumber);

  boolean containerNumberExists(String containerNumber);

  Shipment insertShipment(Shipment shipment);

  Shipment updateShipment(Shipment shipment);

  Optional<Shipment> findShipment(UUID shipmentId);

  List<Shipment> listShipments(UUID companyId);

  LandedCostDraft insertLandedCostDraft(LandedCostDraft draft);

  Optional<LandedCostDraft> findLandedCostDraft(UUID draftId);
}
