package com.newland.erp.finance.application.posting;

import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
