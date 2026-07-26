package com.newland.erp.logistics.application;

import com.newland.erp.logistics.domain.LandedCostDraft;
import com.newland.erp.logistics.domain.Shipment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LogisticsRepository {
  boolean shipmentNumberExists(String shipmentNumber);

  boolean containerNumberExists(String containerNumber);

  boolean insertShipmentIfAbsent(Shipment shipment);

  Optional<Shipment> findShipmentByIdempotencyKey(String idempotencyKey);

  Shipment updateShipment(Shipment shipment);

  Optional<Shipment> findShipment(UUID shipmentId);

  List<Shipment> listShipments(UUID companyId);

  boolean insertLandedCostDraftIfAbsent(LandedCostDraft draft);

  Optional<LandedCostDraft> findLandedCostDraft(UUID draftId);

  Optional<LandedCostDraft> findLandedCostDraftByIdempotencyKey(String idempotencyKey);
}
