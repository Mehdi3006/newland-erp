package com.newland.erp.finance.application.posting;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newland.erp.finance.posting.domain.AccountingEvent;
import com.newland.erp.finance.posting.domain.PostingException;
import com.newland.erp.finance.posting.domain.PostingRule;
import com.newland.erp.finance.posting.domain.PostingRuleLine;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class PostingDomainTest {
  @Test
  void comparesEveryCallerControlledIdempotencyFieldButNotTheIngestionTimestamp() {
    final AccountingEvent accepted = event();

    assertTrue(
        copy(
                accepted,
                accepted.eventId(),
                accepted.companyId(),
                new BigDecimal("100.000"),
                accepted.occurredAt().plusSeconds(10))
            .hasSameIdempotencyPayload(accepted));
    assertFalse(
        copy(
                accepted,
                UUID.randomUUID(),
                accepted.companyId(),
                accepted.amount(),
                accepted.occurredAt())
            .hasSameIdempotencyPayload(accepted));
    assertFalse(
        copy(
                accepted,
                accepted.eventId(),
                UUID.randomUUID(),
                accepted.amount(),
                accepted.occurredAt())
            .hasSameIdempotencyPayload(accepted));
    assertFalse(
        copy(
                accepted,
                accepted.eventId(),
                accepted.companyId(),
                accepted.amount().add(BigDecimal.ONE),
                accepted.occurredAt())
            .hasSameIdempotencyPayload(accepted));
  }

  @Test
  void rejectsInvalidEventSnapshots() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new AccountingEvent(
                UUID.randomUUID(),
                "key",
                "type",
                "source",
                "doc",
                UUID.randomUUID(),
                "1",
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDate.now(),
                LocalDate.now(),
                "USD",
                BigDecimal.ZERO,
                BigDecimal.ONE,
                null,
                null,
                "desc",
                Map.of(),
                Map.of(),
                Instant.now(),
                "actor",
                1));
  }

  @Test
  void rejectsInvertedRuleDatesAndInvalidConstant() {
    final PostingRuleLine line =
        new PostingRuleLine(
            UUID.randomUUID(),
            1,
            PostingRuleLine.Direction.DEBIT,
            PostingRuleLine.AccountResolutionType.FIXED_ACCOUNT,
            UUID.randomUUID(),
            null,
            PostingRuleLine.AmountExpression.EVENT_AMOUNT,
            null,
            "amount",
            Map.of());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PostingRule(
                UUID.randomUUID(),
                "R",
                "Rule",
                "EVENT",
                null,
                LocalDate.now(),
                LocalDate.now().minusDays(1),
                1,
                PostingRule.Status.DRAFT,
                1,
                List.of(line, line),
                Instant.now(),
                "actor",
                Instant.now(),
                "actor"));
    assertThrows(
        PostingException.class,
        () ->
            new PostingRuleLine(
                UUID.randomUUID(),
                1,
                PostingRuleLine.Direction.DEBIT,
                PostingRuleLine.AccountResolutionType.FIXED_ACCOUNT,
                UUID.randomUUID(),
                null,
                PostingRuleLine.AmountExpression.CONSTANT,
                BigDecimal.ONE.negate(),
                "amount",
                Map.of()));
  }

  private static AccountingEvent event() {
    return new AccountingEvent(
        UUID.randomUUID(),
        "posting-key",
        "SALES_ORDER_APPROVED",
        "sales",
        "SALES_ORDER",
        UUID.randomUUID(),
        "SO-100",
        UUID.randomUUID(),
        UUID.randomUUID(),
        LocalDate.parse("2026-07-22"),
        LocalDate.parse("2026-07-22"),
        "USD",
        BigDecimal.ONE,
        new BigDecimal("100.00"),
        BigDecimal.TEN,
        new BigDecimal("90.00"),
        "Posting event",
        Map.of("channel", "ONLINE"),
        Map.of("source", "sales"),
        Instant.parse("2026-07-22T00:00:00Z"),
        "actor",
        1);
  }

  private static AccountingEvent copy(
      final AccountingEvent source,
      final UUID eventId,
      final UUID companyId,
      final BigDecimal amount,
      final Instant occurredAt) {
    return new AccountingEvent(
        eventId,
        source.idempotencyKey(),
        source.eventType(),
        source.sourceModule(),
        source.sourceDocumentType(),
        source.sourceDocumentId(),
        source.sourceDocumentNumber(),
        companyId,
        source.branchId(),
        source.eventDate(),
        source.accountingDate(),
        source.currencyCode(),
        source.exchangeRate(),
        amount,
        source.taxAmount(),
        source.netAmount(),
        source.description(),
        source.dimensions(),
        source.attributes(),
        occurredAt,
        source.submittedBy(),
        source.version());
  }
}
