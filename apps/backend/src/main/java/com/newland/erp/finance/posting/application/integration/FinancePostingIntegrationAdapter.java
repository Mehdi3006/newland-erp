package com.newland.erp.finance.posting.application.integration;

import com.newland.erp.finance.posting.application.FinancialPostingPort;
import com.newland.erp.finance.posting.domain.AccountingEvent;
import com.newland.erp.finance.posting.domain.PostingResult;
import org.springframework.stereotype.Component;

/** Maps the published integration contract to the internal Finance posting domain. */
@Component
public final class FinancePostingIntegrationAdapter implements FinancePostingIntegrationPort {
  private final FinancialPostingPort posting;

  public FinancePostingIntegrationAdapter(final FinancialPostingPort financialPostingPort) {
    posting = financialPostingPort;
  }

  @Override
  public PostingReceipt publish(final AccountingEventMessage message) {
    return receipt(
        posting.submitAccepted(
            new AccountingEvent(
                message.eventId(),
                message.idempotencyKey(),
                message.eventType(),
                message.sourceModule(),
                message.sourceDocumentType(),
                message.sourceDocumentId(),
                message.sourceDocumentNumber(),
                message.companyId(),
                message.branchId(),
                message.eventDate(),
                message.accountingDate(),
                message.currencyCode(),
                message.exchangeRate(),
                message.amount(),
                message.taxAmount(),
                message.netAmount(),
                message.description(),
                message.dimensions(),
                message.attributes(),
                message.occurredAt(),
                message.submittedBy(),
                0)));
  }

  @Override
  public PostingReceipt retry(final java.util.UUID postingRequestId) {
    return receipt(posting.retry(postingRequestId));
  }

  private static PostingReceipt receipt(final PostingResult result) {
    return new PostingReceipt(
        result.postingRequestId(),
        result.eventId(),
        result.status().name(),
        result.journalEntryId(),
        result.journalNumber(),
        result.failureCode(),
        result.failureMessage());
  }
}
