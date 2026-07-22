package com.newland.erp.finance.posting.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.newland.erp.finance.posting.domain.PostingRule;
import com.newland.erp.finance.posting.domain.PostingRuleLine;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class PostingRulePrecedenceTest {
  @Test
  void companyRuleSetCompletelyPrecedesGlobalRuleSet() {
    final InMemoryPostingRepository repository = new InMemoryPostingRepository();
    final UUID companyId = UUID.randomUUID();
    final PostingRule global = rule("GLOBAL", null, 100);
    final PostingRule company = rule("COMPANY", companyId, 1);
    repository.save(global);
    repository.save(company);

    assertThat(repository.findApplicable("EVENT", companyId, LocalDate.parse("2026-07-22")))
        .containsExactly(company);
    assertThat(
            repository.findApplicable(
                "EVENT", UUID.randomUUID(), LocalDate.parse("2026-07-22")))
        .containsExactly(global);
  }

  private PostingRule rule(final String code, final UUID companyId, final int priority) {
    return new PostingRule(
        UUID.randomUUID(),
        code,
        code,
        "EVENT",
        companyId,
        LocalDate.parse("2026-01-01"),
        null,
        priority,
        PostingRule.Status.ACTIVE,
        1,
        List.of(
            line(1, PostingRuleLine.Direction.DEBIT),
            line(2, PostingRuleLine.Direction.CREDIT)),
        Instant.parse("2026-01-01T00:00:00Z"),
        "actor",
        null,
        null);
  }

  private PostingRuleLine line(
      final int lineNumber, final PostingRuleLine.Direction direction) {
    return new PostingRuleLine(
        UUID.randomUUID(),
        lineNumber,
        direction,
        PostingRuleLine.AccountResolutionType.FIXED_ACCOUNT,
        UUID.randomUUID(),
        null,
        PostingRuleLine.AmountExpression.CONSTANT,
        BigDecimal.ONE,
        "line",
        Map.of());
  }
}
