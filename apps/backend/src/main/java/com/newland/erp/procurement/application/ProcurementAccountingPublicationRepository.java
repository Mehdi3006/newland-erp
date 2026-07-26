package com.newland.erp.procurement.application;

import com.newland.erp.procurement.domain.ProcurementAccountingEvent;
import java.util.Optional;
import java.util.UUID;

public interface ProcurementAccountingPublicationRepository {
  boolean insertIfAbsent(ProcurementAccountingEvent event);

  Optional<Publication> findByEventId(UUID eventId);

  Optional<Publication> findByIdempotencyKey(String idempotencyKey);

  void complete(UUID eventId, ProcurementAccountingService.PostingReceipt receipt);

  record Publication(
      ProcurementAccountingEvent event,
      String status,
      UUID postingRequestId,
      UUID journalEntryId,
      String journalNumber,
      String failureCode,
      String failureMessage) {}
}
