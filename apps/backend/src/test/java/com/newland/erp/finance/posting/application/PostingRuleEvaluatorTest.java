package com.newland.erp.finance.posting.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.newland.erp.finance.domain.JournalEntry;
import com.newland.erp.finance.posting.domain.AccountingEvent;
import com.newland.erp.finance.posting.domain.PostingException;
import com.newland.erp.finance.posting.domain.PostingRule;
import com.newland.erp.finance.posting.domain.PostingRuleLine;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class PostingRuleEvaluatorTest {
  private static final UUID COMPANY_ID = UUID.randomUUID();
  private static final UUID FIXED_ACCOUNT_ID = UUID.randomUUID();
  private static final UUID ATTRIBUTE_ACCOUNT_ID = UUID.randomUUID();
  private static final UUID COST_CENTER_ID = UUID.randomUUID();
  private static final UUID PROFIT_CENTER_ID = UUID.randomUUID();

  @Test
  void evaluatesAllAmountExpressionsAndResolvesAccountsAndDimensions() {
    final TrackingPorts ports = new TrackingPorts();
    final PostingRuleEvaluator evaluator =
        new PostingRuleEvaluator(ports, ports, ports, ports);
    final AccountingEvent event = event();
    final PostingRule rule = rule();

    final List<JournalEntry.JournalLine> lines = evaluator.evaluate(event, rule);

    assertThat(lines).hasSize(5);
    assertThat(lines)
        .extracting(JournalEntry.JournalLine::debit)
        .containsExactly(
            amount("100"), amount("0"), amount("10"), amount("0"), amount("7.5"));
    assertThat(lines)
        .extracting(JournalEntry.JournalLine::credit)
        .containsExactly(
            amount("0"), amount("90"), amount("0"), amount("25"), amount("0"));
    assertThat(lines.get(0).accountId()).isEqualTo(FIXED_ACCOUNT_ID);
    assertThat(lines.get(1).accountId()).isEqualTo(ATTRIBUTE_ACCOUNT_ID);
    assertThat(lines.get(0).costCenterId()).isEqualTo(COST_CENTER_ID);
    assertThat(lines.get(0).profitCenterId()).isEqualTo(PROFIT_CENTER_ID);
    assertThat(lines.get(0).dimensionCode()).isEqualTo("CHANNEL-ONLINE");
    assertThat(ports.requiredAccounts).hasSize(4).containsOnly(FIXED_ACCOUNT_ID);
    assertThat(ports.resolvedAccountKeys).containsExactly("revenueAccountId");
    assertThat(ports.requiredCostCenters).containsExactly(COST_CENTER_ID);
    assertThat(ports.requiredProfitCenters).containsExactly(PROFIT_CENTER_ID);
    assertThat(ports.requiredDimensions).containsExactly("CHANNEL-ONLINE");
  }

  @Test
  void rejectsMissingCostAmountAndInvalidMappedIdentifiers() {
    final TrackingPorts ports = new TrackingPorts();
    final PostingRuleEvaluator evaluator =
        new PostingRuleEvaluator(ports, ports, ports, ports);
    final AccountingEvent withoutCost =
        accountingEvent(Map.of("revenueAccountId", ATTRIBUTE_ACCOUNT_ID.toString()), dimensions());
    final PostingRule costRule =
        postingRule(
            List.of(
                line(
                    1,
                    PostingRuleLine.Direction.DEBIT,
                    PostingRuleLine.AccountResolutionType.FIXED_ACCOUNT,
                    PostingRuleLine.AmountExpression.EVENT_COST_AMOUNT,
                    Map.of()),
                line(
                    2,
                    PostingRuleLine.Direction.CREDIT,
                    PostingRuleLine.AccountResolutionType.FIXED_ACCOUNT,
                    PostingRuleLine.AmountExpression.EVENT_COST_AMOUNT,
                    Map.of())));
    assertThatThrownBy(() -> evaluator.evaluate(withoutCost, costRule))
        .isInstanceOf(PostingException.class)
        .hasMessageContaining("costAmount");

    final AccountingEvent invalidDimension =
        accountingEvent(
            attributes(),
            Map.of(
                "costCenterKey",
                "not-a-uuid",
                "profitCenterKey",
                PROFIT_CENTER_ID.toString(),
                "dimensionKey",
                "CHANNEL-ONLINE"));
    assertThatThrownBy(() -> evaluator.evaluate(invalidDimension, rule()))
        .isInstanceOf(PostingException.class)
        .hasMessageContaining("costCenter");
  }

  private static PostingRule rule() {
    return postingRule(
        List.of(
            line(
                1,
                PostingRuleLine.Direction.DEBIT,
                PostingRuleLine.AccountResolutionType.FIXED_ACCOUNT,
                PostingRuleLine.AmountExpression.EVENT_AMOUNT,
                dimensionMappings()),
            line(
                2,
                PostingRuleLine.Direction.CREDIT,
                PostingRuleLine.AccountResolutionType.EVENT_ATTRIBUTE_ACCOUNT,
                PostingRuleLine.AmountExpression.EVENT_NET_AMOUNT,
                Map.of()),
            line(
                3,
                PostingRuleLine.Direction.DEBIT,
                PostingRuleLine.AccountResolutionType.FIXED_ACCOUNT,
                PostingRuleLine.AmountExpression.EVENT_TAX_AMOUNT,
                Map.of()),
            line(
                4,
                PostingRuleLine.Direction.CREDIT,
                PostingRuleLine.AccountResolutionType.FIXED_ACCOUNT,
                PostingRuleLine.AmountExpression.EVENT_COST_AMOUNT,
                Map.of()),
            constantLine(5)));
  }

  private static PostingRule postingRule(final List<PostingRuleLine> lines) {
    return new PostingRule(
        UUID.randomUUID(),
        "EVALUATOR",
        "Evaluator rule",
        "TEST_EVENT",
        COMPANY_ID,
        LocalDate.parse("2026-01-01"),
        null,
        100,
        PostingRule.Status.ACTIVE,
        1,
        lines,
        Instant.parse("2026-07-22T00:00:00Z"),
        "architect",
        null,
        null);
  }

  private static PostingRuleLine line(
      final int lineNumber,
      final PostingRuleLine.Direction direction,
      final PostingRuleLine.AccountResolutionType accountType,
      final PostingRuleLine.AmountExpression expression,
      final Map<String, String> mappings) {
    return new PostingRuleLine(
        UUID.randomUUID(),
        lineNumber,
        direction,
        accountType,
        accountType == PostingRuleLine.AccountResolutionType.FIXED_ACCOUNT
            ? FIXED_ACCOUNT_ID
            : null,
        accountType == PostingRuleLine.AccountResolutionType.EVENT_ATTRIBUTE_ACCOUNT
            ? "revenueAccountId"
            : null,
        expression,
        null,
        "line",
        mappings);
  }

  private static PostingRuleLine constantLine(final int lineNumber) {
    return new PostingRuleLine(
        UUID.randomUUID(),
        lineNumber,
        PostingRuleLine.Direction.DEBIT,
        PostingRuleLine.AccountResolutionType.FIXED_ACCOUNT,
        FIXED_ACCOUNT_ID,
        null,
        PostingRuleLine.AmountExpression.CONSTANT,
        new BigDecimal("7.5"),
        "constant",
        Map.of());
  }

  private static AccountingEvent event() {
    return accountingEvent(attributes(), dimensions());
  }

  private static AccountingEvent accountingEvent(
      final Map<String, String> attributes, final Map<String, String> dimensions) {
    return new AccountingEvent(
        UUID.randomUUID(),
        "evaluator-event",
        "TEST_EVENT",
        "test",
        "TEST_DOCUMENT",
        UUID.randomUUID(),
        "TEST-1",
        COMPANY_ID,
        UUID.randomUUID(),
        LocalDate.parse("2026-07-22"),
        LocalDate.parse("2026-07-22"),
        "USD",
        BigDecimal.ONE,
        new BigDecimal("100"),
        new BigDecimal("10"),
        new BigDecimal("90"),
        "test",
        dimensions,
        attributes,
        Instant.parse("2026-07-22T00:00:00Z"),
        "architect",
        1);
  }

  private static Map<String, String> attributes() {
    return Map.of(
        "revenueAccountId", ATTRIBUTE_ACCOUNT_ID.toString(), "costAmount", "25");
  }

  private static Map<String, String> dimensions() {
    return Map.of(
        "costCenterKey",
        COST_CENTER_ID.toString(),
        "profitCenterKey",
        PROFIT_CENTER_ID.toString(),
        "dimensionKey",
        "CHANNEL-ONLINE");
  }

  private static Map<String, String> dimensionMappings() {
    return Map.of(
        "costCenter", "costCenterKey",
        "profitCenter", "profitCenterKey",
        "dimension", "dimensionKey");
  }

  private static BigDecimal amount(final String value) {
    return new BigDecimal(value).setScale(6);
  }

  private static final class TrackingPorts
      implements PostingPorts.AccountResolutionPort,
          PostingPorts.CostCenterValidationPort,
          PostingPorts.ProfitCenterValidationPort,
          PostingPorts.FinancialDimensionValidationPort {
    private final List<UUID> requiredAccounts = new ArrayList<>();
    private final List<String> resolvedAccountKeys = new ArrayList<>();
    private final List<UUID> requiredCostCenters = new ArrayList<>();
    private final List<UUID> requiredProfitCenters = new ArrayList<>();
    private final List<String> requiredDimensions = new ArrayList<>();

    @Override
    public void requireAccount(final UUID companyId, final UUID accountId) {
      assertThat(companyId).isEqualTo(COMPANY_ID);
      requiredAccounts.add(accountId);
    }

    @Override
    public UUID resolveAttribute(
        final UUID companyId,
        final String key,
        final Map<String, String> attributes) {
      assertThat(companyId).isEqualTo(COMPANY_ID);
      resolvedAccountKeys.add(key);
      return UUID.fromString(attributes.get(key));
    }

    @Override
    public void requireCostCenter(final UUID companyId, final UUID costCenterId) {
      assertThat(companyId).isEqualTo(COMPANY_ID);
      requiredCostCenters.add(costCenterId);
    }

    @Override
    public void requireProfitCenter(final UUID companyId, final UUID profitCenterId) {
      assertThat(companyId).isEqualTo(COMPANY_ID);
      requiredProfitCenters.add(profitCenterId);
    }

    @Override
    public void requireDimensions(
        final UUID companyId, final Map<String, String> dimensions) {
      assertThat(companyId).isEqualTo(COMPANY_ID);
    }

    @Override
    public void requireDimension(final UUID companyId, final String dimensionCode) {
      assertThat(companyId).isEqualTo(COMPANY_ID);
      requiredDimensions.add(dimensionCode);
    }
  }
}
