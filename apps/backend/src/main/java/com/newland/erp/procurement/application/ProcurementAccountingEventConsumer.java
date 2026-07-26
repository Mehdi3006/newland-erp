package com.newland.erp.procurement.application;

import com.newland.erp.finance.posting.application.integration.FinancePostingIntegrationPort;
import com.newland.erp.platform.application.integration.PlatformOutboxConsumer;
import com.newland.erp.procurement.domain.ProcurementAccountingEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Consumes committed Procurement facts and delegates all accounting decisions to Finance. */
@Component
public final class ProcurementAccountingEventConsumer implements PlatformOutboxConsumer {
  private final ProcurementAccountingPublicationRepository publications;
  private final FinancePostingIntegrationPort finance;

  public ProcurementAccountingEventConsumer(
      final ProcurementAccountingPublicationRepository publicationRepository,
      final FinancePostingIntegrationPort financePostingPort) {
    publications = publicationRepository;
    finance = financePostingPort;
  }

  @Override
  public boolean supports(final String sourceContext, final String eventType) {
    return "procurement".equals(sourceContext)
        && ProcurementAccountingService.OUTBOX_EVENT.equals(eventType);
  }

  @Override
  @Transactional
  public void consume(final OutboxEvent outboxEvent) {
    final var publication =
        publications
            .findByEventId(outboxEvent.aggregateId())
            .orElseThrow(() -> new IllegalStateException("Procurement publication not found."));
    if (!"PENDING".equals(publication.status())) {
      return;
    }
    final ProcurementAccountingEvent event = publication.event();
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
    if ("FAILED".equals(receipt.status())) {
      throw new IllegalStateException("Finance posting failed and will be retried.");
    }
    publications.complete(
        event.eventId(),
        new ProcurementAccountingService.PostingReceipt(
            receipt.postingRequestId(),
            receipt.accountingEventId(),
            receipt.status(),
            receipt.journalEntryId(),
            receipt.journalNumber(),
            receipt.failureCode(),
            receipt.failureMessage()));
  }
}
