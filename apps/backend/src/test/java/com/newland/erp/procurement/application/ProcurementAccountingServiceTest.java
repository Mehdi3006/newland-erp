package com.newland.erp.procurement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.newland.erp.finance.posting.application.integration.FinancePostingIntegrationPort;
import com.newland.erp.platform.application.integration.PlatformAuditOutboxPort;
import com.newland.erp.procurement.domain.ProcurementAccountingEvent;
import com.newland.erp.procurement.domain.ProcurementAccountingEventTest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class ProcurementAccountingServiceTest {
  @Test
  void publishesOnlyTheFiveApprovedEventsWithProcurementSource() {
    final Fixture fixture = fixture(true, false);

    for (final ProcurementAccountingEvent.EventType type
        : ProcurementAccountingEvent.EventType.values()) {
      fixture.service.publish(event(type));
    }

    assertThat(fixture.finance.eventTypes)
        .containsExactly(
            "PurchaseOrderApproved",
            "GoodsReceived",
            "SupplierInvoicePosted",
            "SupplierCreditNotePosted",
            "SupplierPaymentPosted");
    assertThat(fixture.finance.sourceModules).containsOnly("PROCUREMENT");
    assertThat(fixture.platform.audits).hasSize(5);
    assertThat(fixture.platform.events).hasSize(5);
  }

  @Test
  void purchaseOrderPostingIsDisabledUnlessFeatureFlagIsEnabled() {
    final Fixture disabled = fixture(false, false);

    assertThatThrownBy(
            () ->
                disabled.service.publish(
                    event(ProcurementAccountingEvent.EventType.PURCHASE_ORDER_APPROVED)))
        .isInstanceOf(IllegalStateException.class);
    assertThat(disabled.finance.calls).hasValue(0);
  }

  @Test
  void duplicateAndConcurrentPublicationCreateOnePostingRequestAndJournal() throws Exception {
    final Fixture fixture = fixture(true, false);
    final ProcurementAccountingEvent event =
        event(ProcurementAccountingEvent.EventType.SUPPLIER_INVOICE_POSTED);
    final CountDownLatch start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(8)) {
      final var futures =
          java.util.stream.IntStream.range(0, 8)
              .mapToObj(
                  ignored ->
                      executor.submit(
                          () -> {
                            start.await();
                            return fixture.service.publish(event);
                          }))
              .toList();
      start.countDown();
      for (final var future : futures) {
        assertThat(future.get().postingRequestId()).isNotNull();
      }
    }

    assertThat(fixture.finance.postingRequests).hasSize(1);
    assertThat(fixture.finance.journals).hasSize(1);
  }

  @Test
  void retryUsesSamePostingRequestAndFailureRollsBackPublicationSideEffects() {
    final Fixture fixture = fixture(true, false);
    final var posted =
        fixture.service.publish(
            event(ProcurementAccountingEvent.EventType.SUPPLIER_CREDIT_NOTE_POSTED));
    final var retried =
        fixture.service.retry(
            posted.postingRequestId(), fixture.companyId, fixture.actor);

    assertThat(retried.postingRequestId()).isEqualTo(posted.postingRequestId());
    assertThat(fixture.finance.journals).hasSize(1);

    final Fixture failing = fixture(true, true);
    assertThatThrownBy(
            () ->
                failing.service.publish(
                    event(ProcurementAccountingEvent.EventType.GOODS_RECEIVED)))
        .isInstanceOf(IllegalStateException.class);
    assertThat(failing.platform.audits).isEmpty();
    assertThat(failing.platform.events).isEmpty();
  }

  private static ProcurementAccountingEvent event(
      final ProcurementAccountingEvent.EventType type) {
    return ProcurementAccountingEventTest.event(type, UUID.randomUUID(), UUID.randomUUID());
  }

  private static Fixture fixture(final boolean purchaseOrderEnabled, final boolean failFinance) {
    final RecordingFinance finance = new RecordingFinance(failFinance);
    final RecordingPlatform platform = new RecordingPlatform();
    final ProcurementAccountingEvent sample =
        event(ProcurementAccountingEvent.EventType.GOODS_RECEIVED);
    final ProcurementAccountingPorts.SecurityPort security =
        new ProcurementAccountingPorts.SecurityPort() {
          @Override
          public String currentActor() {
            return sample.actor();
          }

          @Override
          public void requireCompanyCapability(
              final String actor, final String capability, final UUID companyId) {
            if (actor == null || actor.isBlank() || companyId == null) {
              throw new IllegalArgumentException("unauthorized");
            }
          }
        };
    return new Fixture(
        new ProcurementAccountingService(
            finance, platform, key -> purchaseOrderEnabled, security),
        finance,
        platform,
        sample.companyId(),
        sample.actor());
  }

  private record Fixture(
      ProcurementAccountingService service,
      RecordingFinance finance,
      RecordingPlatform platform,
      UUID companyId,
      String actor) {}

  private static final class RecordingFinance implements FinancePostingIntegrationPort {
    private final Map<UUID, PostingReceipt> postingRequests = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> journals = new ConcurrentHashMap<>();
    private final List<String> eventTypes = java.util.Collections.synchronizedList(new ArrayList<>());
    private final List<String> sourceModules = java.util.Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger calls = new AtomicInteger();
    private final boolean fail;

    RecordingFinance(final boolean failPublication) {
      fail = failPublication;
    }

    @Override
    public PostingReceipt publish(final AccountingEventMessage message) {
      calls.incrementAndGet();
      if (fail) {
        throw new IllegalStateException("finance unavailable");
      }
      eventTypes.add(message.eventType());
      sourceModules.add(message.sourceModule());
      return postingRequests.computeIfAbsent(
          message.eventId(),
          id -> {
            final UUID journalId = UUID.randomUUID();
            journals.put(id, journalId);
            return new PostingReceipt(
                UUID.randomUUID(), id, "POSTED", journalId, "JE-1", null, null);
          });
    }

    @Override
    public PostingReceipt retry(final UUID postingRequestId) {
      return postingRequests.values().stream()
          .filter(receipt -> receipt.postingRequestId().equals(postingRequestId))
          .findFirst()
          .orElseThrow();
    }
  }

  private static final class RecordingPlatform implements PlatformAuditOutboxPort {
    private final List<String> audits =
        java.util.Collections.synchronizedList(new ArrayList<>());
    private final List<String> events =
        java.util.Collections.synchronizedList(new ArrayList<>());

    @Override
    public void recordAudit(
        final String actor,
        final String action,
        final String targetType,
        final UUID targetId,
        final Map<String, String> attributes) {
      audits.add(action);
    }

    @Override
    public void publishEvent(
        final String sourceContext,
        final String eventType,
        final UUID aggregateId,
        final Map<String, String> payload) {
      events.add(eventType);
    }

    @Override
    public void attachFile(
        final String ownerContext,
        final String ownerType,
        final UUID ownerId,
        final UUID fileId) {}
  }
}
