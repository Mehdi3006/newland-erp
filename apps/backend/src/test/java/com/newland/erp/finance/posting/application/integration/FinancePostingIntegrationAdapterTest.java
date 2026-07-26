package com.newland.erp.finance.posting.application.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.newland.erp.finance.posting.application.FinancialPostingPort;
import com.newland.erp.finance.posting.domain.AccountingEvent;
import com.newland.erp.finance.posting.domain.PostingRequest;
import com.newland.erp.finance.posting.domain.PostingResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

final class FinancePostingIntegrationAdapterTest {
  @Test
  void mapsPublishedAccountingFactToTheRealFinancePostingPort() {
    final AtomicReference<AccountingEvent> captured = new AtomicReference<>();
    final UUID requestId = UUID.randomUUID();
    final UUID journalId = UUID.randomUUID();
    final FinancialPostingPort posting =
        new StubPostingPort(captured, requestId, journalId);
    final FinancePostingIntegrationAdapter adapter =
        new FinancePostingIntegrationAdapter(posting);
    final var message = message();

    final var receipt = adapter.publish(message);

    assertThat(captured.get())
        .usingRecursiveComparison()
        .ignoringFields("version")
        .isEqualTo(
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
                0));
    assertThat(receipt.postingRequestId()).isEqualTo(requestId);
    assertThat(receipt.journalEntryId()).isEqualTo(journalId);
    assertThat(receipt.status()).isEqualTo("POSTED");
  }

  @Test
  void delegatesRetryWithoutCreatingAnotherIntegrationPath() {
    final UUID requestId = UUID.randomUUID();
    final UUID journalId = UUID.randomUUID();
    final StubPostingPort posting =
        new StubPostingPort(new AtomicReference<>(), requestId, journalId);

    final var receipt = new FinancePostingIntegrationAdapter(posting).retry(requestId);

    assertThat(posting.retriedRequestId).isEqualTo(requestId);
    assertThat(receipt.journalEntryId()).isEqualTo(journalId);
  }

  private static FinancePostingIntegrationPort.AccountingEventMessage message() {
    return new FinancePostingIntegrationPort.AccountingEventMessage(
        UUID.randomUUID(),
        "procurement:invoice:100",
        "SupplierInvoicePosted",
        "PROCUREMENT",
        "SUPPLIER_INVOICE",
        UUID.randomUUID(),
        "INV-100",
        UUID.randomUUID(),
        UUID.randomUUID(),
        LocalDate.of(2026, 7, 26),
        LocalDate.of(2026, 7, 26),
        "EUR",
        new BigDecimal("1.2500000000"),
        new BigDecimal("120.00"),
        new BigDecimal("20.00"),
        new BigDecimal("100.00"),
        "Supplier invoice accepted",
        Map.of("costCenterId", UUID.randomUUID().toString()),
        Map.of("supplierId", UUID.randomUUID().toString()),
        Instant.parse("2026-07-26T00:00:00Z"),
        UUID.randomUUID().toString());
  }

  private static final class StubPostingPort implements FinancialPostingPort {
    private final AtomicReference<AccountingEvent> captured;
    private final UUID requestId;
    private final UUID journalId;
    private UUID retriedRequestId;

    StubPostingPort(
        final AtomicReference<AccountingEvent> capturedEvent,
        final UUID postingRequestId,
        final UUID journalEntryId) {
      captured = capturedEvent;
      requestId = postingRequestId;
      journalId = journalEntryId;
    }

    @Override
    public PostingResult submit(final AccountingEvent event) {
      captured.set(event);
      return result(event.eventId());
    }

    @Override
    public PostingResult preview(final AccountingEvent event) {
      throw new UnsupportedOperationException();
    }

    @Override
    public PostingRequest status(final UUID postingRequestId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public PostingResult retry(final UUID postingRequestId) {
      retriedRequestId = postingRequestId;
      return result(UUID.randomUUID());
    }

    private PostingResult result(final UUID eventId) {
      return new PostingResult(
          requestId,
          eventId,
          PostingRequest.Status.POSTED,
          journalId,
          "JE-100",
          null,
          null);
    }
  }
}
