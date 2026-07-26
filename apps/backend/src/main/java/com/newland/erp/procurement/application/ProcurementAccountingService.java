package com.newland.erp.procurement.application;

import com.newland.erp.platform.application.integration.PlatformAuditOutboxPort;
import com.newland.erp.platform.application.integration.PlatformFeatureFlagPort;
import com.newland.erp.procurement.domain.ProcurementAccountingEvent;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Publishes Procurement facts without embedding any accounting rule or journal knowledge. */
@Service
public final class ProcurementAccountingService {
  static final String POST_CAPABILITY = "procurement.finance.post";
  static final String RETRY_CAPABILITY = "procurement.finance.retry";
  static final String PURCHASE_ORDER_FLAG =
      "procurement.finance.purchase-order-approved";
  static final String OUTBOX_EVENT = "ProcurementAccountingEventPublished";
  private final ProcurementAccountingPublicationRepository publications;
  private final PlatformAuditOutboxPort platform;
  private final PlatformFeatureFlagPort featureFlags;
  private final ProcurementAccountingPorts.SecurityPort security;

  public ProcurementAccountingService(
      final ProcurementAccountingPublicationRepository publicationRepository,
      final PlatformAuditOutboxPort platformPort,
      final PlatformFeatureFlagPort featureFlagPort,
      final ProcurementAccountingPorts.SecurityPort securityPort) {
    publications = publicationRepository;
    platform = platformPort;
    featureFlags = featureFlagPort;
    security = securityPort;
  }

  @Transactional
  public PostingReceipt publish(final ProcurementAccountingEvent event) {
    security.requireCompanyCapability(event.actor(), POST_CAPABILITY, event.companyId());
    if (event.eventType() == ProcurementAccountingEvent.EventType.PURCHASE_ORDER_APPROVED
        && !featureFlags.isEnabled(PURCHASE_ORDER_FLAG)) {
      throw new IllegalStateException(
          "PurchaseOrderApproved finance posting is disabled by feature flag.");
    }
    final var existing = publications.findByIdempotencyKey(event.idempotencyKey());
    if (existing.isPresent()) {
      return existing(event, existing.get());
    }
    final var sameId = publications.findByEventId(event.eventId());
    if (sameId.isPresent()) {
      return existing(event, sameId.get());
    }
    if (!publications.insertIfAbsent(event)) {
      return existing(
          event,
          publications
              .findByIdempotencyKey(event.idempotencyKey())
              .or(() -> publications.findByEventId(event.eventId()))
              .orElseThrow());
    }
    platform.publishEvent(
        event.eventId(),
        "procurement",
        OUTBOX_EVENT,
        event.eventId(),
        Map.of(
            "idempotencyKey", event.idempotencyKey(),
            "eventType", event.eventType().financeEventType()));
    recordPublication(event.actor(), event.eventType().financeEventType(), event.eventId());
    return PostingReceipt.pending(event.eventId());
  }

  @Transactional
  public PostingReceipt retry(
      final UUID accountingEventId, final UUID companyId, final String actor) {
    security.requireCompanyCapability(actor, RETRY_CAPABILITY, companyId);
    final var publication =
        publications
            .findByEventId(accountingEventId)
            .orElseThrow(() -> new IllegalArgumentException("Accounting event not found."));
    if (!publication.event().companyId().equals(companyId)) {
      throw new org.springframework.security.access.AccessDeniedException(
          "Procurement Finance event belongs to another company.");
    }
    platform.retryEvent(accountingEventId);
    return PostingReceipt.from(publication);
  }

  private void recordPublication(
      final String actor,
      final String eventType,
      final UUID eventId) {
    final Map<String, String> attributes =
        Map.of("eventType", eventType);
    platform.recordAudit(
        actor,
        "PROCUREMENT_ACCOUNTING_EVENT_PUBLISHED",
        "ProcurementAccountingEvent",
        eventId,
        attributes);
  }

  private static PostingReceipt existing(
      final ProcurementAccountingEvent candidate,
      final ProcurementAccountingPublicationRepository.Publication persisted) {
    if (!persisted.event().hasSamePublicationPayload(candidate)) {
      throw new IllegalArgumentException(
          "Idempotency key or event ID was reused with conflicting Procurement data.");
    }
    return PostingReceipt.from(persisted);
  }

  public record PostingReceipt(
      UUID postingRequestId,
      UUID accountingEventId,
      String status,
      UUID journalEntryId,
      String journalNumber,
      String failureCode,
      String failureMessage) {
    static PostingReceipt pending(final UUID eventId) {
      return new PostingReceipt(null, eventId, "PENDING", null, null, null, null);
    }

    static PostingReceipt from(
        final ProcurementAccountingPublicationRepository.Publication publication) {
      return new PostingReceipt(
          publication.postingRequestId(),
          publication.event().eventId(),
          publication.status(),
          publication.journalEntryId(),
          publication.journalNumber(),
          publication.failureCode(),
          publication.failureMessage());
    }
  }
}
