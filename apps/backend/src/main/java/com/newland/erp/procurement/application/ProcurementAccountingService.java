package com.newland.erp.procurement.application;

import com.newland.erp.finance.posting.application.integration.FinancePostingIntegrationPort;
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
  private final FinancePostingIntegrationPort finance;
  private final PlatformAuditOutboxPort platform;
  private final PlatformFeatureFlagPort featureFlags;
  private final ProcurementAccountingPorts.SecurityPort security;

  public ProcurementAccountingService(
      final FinancePostingIntegrationPort financePostingPort,
      final PlatformAuditOutboxPort platformPort,
      final PlatformFeatureFlagPort featureFlagPort,
      final ProcurementAccountingPorts.SecurityPort securityPort) {
    finance = financePostingPort;
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
    final FinancePostingIntegrationPort.PostingReceipt receipt =
        finance.publish(
            new FinancePostingIntegrationPort.AccountingEventMessage(
                event.eventId(),
                event.idempotencyKey(),
                event.eventType().financeEventType(),
                "PROCUREMENT",
                event.referenceDocumentType(),
                event.referenceDocumentId(),
                event.referenceDocumentNumber(),
                event.companyId(),
                event.branchId(),
                event.eventDate(),
                event.accountingDate(),
                event.currencyCode(),
                event.exchangeRate(),
                event.amount(),
                event.taxAmount(),
                event.netAmount(),
                event.description(),
                event.postingDimensions(),
                event.postingAttributes(),
                event.occurredAt(),
                event.actor()));
    recordPublication(event.actor(), event.eventType().financeEventType(), event.eventId(),
        receipt.postingRequestId());
    return PostingReceipt.from(receipt);
  }

  @Transactional
  public PostingReceipt retry(
      final UUID postingRequestId, final UUID companyId, final String actor) {
    security.requireCompanyCapability(actor, RETRY_CAPABILITY, companyId);
    final FinancePostingIntegrationPort.PostingReceipt receipt = finance.retry(postingRequestId);
    platform.recordAudit(
        actor,
        "PROCUREMENT_FINANCE_POSTING_RETRIED",
        "ProcurementAccountingEvent",
        receipt.accountingEventId(),
        Map.of("postingRequestId", postingRequestId.toString()));
    platform.publishEvent(
        "procurement",
        "ProcurementFinancePostingRetried",
        receipt.accountingEventId(),
        Map.of("postingRequestId", postingRequestId.toString()));
    return PostingReceipt.from(receipt);
  }

  private void recordPublication(
      final String actor,
      final String eventType,
      final UUID eventId,
      final UUID postingRequestId) {
    final Map<String, String> attributes =
        Map.of(
            "eventType", eventType,
            "postingRequestId", postingRequestId.toString());
    platform.recordAudit(
        actor,
        "PROCUREMENT_ACCOUNTING_EVENT_PUBLISHED",
        "ProcurementAccountingEvent",
        eventId,
        attributes);
    platform.publishEvent(
        "procurement",
        "ProcurementAccountingEventPublished",
        eventId,
        attributes);
  }

  public record PostingReceipt(
      UUID postingRequestId,
      UUID accountingEventId,
      String status,
      UUID journalEntryId,
      String journalNumber,
      String failureCode,
      String failureMessage) {
    static PostingReceipt from(final FinancePostingIntegrationPort.PostingReceipt receipt) {
      return new PostingReceipt(
          receipt.postingRequestId(),
          receipt.accountingEventId(),
          receipt.status(),
          receipt.journalEntryId(),
          receipt.journalNumber(),
          receipt.failureCode(),
          receipt.failureMessage());
    }
  }
}
