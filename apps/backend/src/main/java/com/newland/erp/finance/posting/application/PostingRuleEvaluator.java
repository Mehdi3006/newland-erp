package com.newland.erp.finance.posting.application;

import com.newland.erp.finance.domain.JournalEntry;
import com.newland.erp.finance.posting.domain.AccountingEvent;
import com.newland.erp.finance.posting.domain.PostingException;
import com.newland.erp.finance.posting.domain.PostingRule;
import com.newland.erp.finance.posting.domain.PostingRuleLine;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Evaluates an active posting rule against an immutable accounting-event snapshot. */
@Component
public final class PostingRuleEvaluator {
  private final PostingPorts.AccountResolutionPort accounts;
  private final PostingPorts.CostCenterValidationPort costCenters;
  private final PostingPorts.ProfitCenterValidationPort profitCenters;
  private final PostingPorts.FinancialDimensionValidationPort dimensions;

  public PostingRuleEvaluator(
      final PostingPorts.AccountResolutionPort accountPort,
      final PostingPorts.CostCenterValidationPort costCenterPort,
      final PostingPorts.ProfitCenterValidationPort profitCenterPort,
      final PostingPorts.FinancialDimensionValidationPort dimensionPort) {
    accounts = accountPort;
    costCenters = costCenterPort;
    profitCenters = profitCenterPort;
    dimensions = dimensionPort;
  }

  public List<JournalEntry.JournalLine> evaluate(
      final AccountingEvent event, final PostingRule rule) {
    dimensions.requireDimensions(event.companyId(), event.dimensions());
    return rule.lines().stream().map(line -> evaluateLine(event, line)).toList();
  }

  private JournalEntry.JournalLine evaluateLine(
      final AccountingEvent event, final PostingRuleLine line) {
    final BigDecimal amount = amount(event, line);
    final UUID accountId = account(event, line);
    final UUID costCenterId = mappedUuid(event, line, "costCenter");
    final UUID profitCenterId = mappedUuid(event, line, "profitCenter");
    final String dimensionCode = mappedValue(event, line, "dimension");
    if (costCenterId != null) {
      costCenters.requireCostCenter(event.companyId(), costCenterId);
    }
    if (profitCenterId != null) {
      profitCenters.requireProfitCenter(event.companyId(), profitCenterId);
    }
    if (dimensionCode != null) {
      dimensions.requireDimension(event.companyId(), dimensionCode);
    }
    return new JournalEntry.JournalLine(
        lineId(event.eventId(), line.id()),
        accountId,
        line.direction() == PostingRuleLine.Direction.DEBIT ? amount : BigDecimal.ZERO,
        line.direction() == PostingRuleLine.Direction.CREDIT ? amount : BigDecimal.ZERO,
        costCenterId,
        profitCenterId,
        dimensionCode,
        null,
        amount,
        event.exchangeRate());
  }

  private static UUID lineId(final UUID eventId, final UUID ruleLineId) {
    return UUID.nameUUIDFromBytes(
        (eventId + ":" + ruleLineId).getBytes(StandardCharsets.UTF_8));
  }

  private UUID account(final AccountingEvent event, final PostingRuleLine line) {
    if (line.accountResolutionType() == PostingRuleLine.AccountResolutionType.FIXED_ACCOUNT) {
      accounts.requireAccount(event.companyId(), line.fixedAccountId());
      return line.fixedAccountId();
    }
    return accounts.resolveAttribute(
        event.companyId(), line.accountAttributeKey(), event.attributes());
  }

  private static BigDecimal amount(
      final AccountingEvent event, final PostingRuleLine line) {
    final BigDecimal value =
        switch (line.amountExpression()) {
          case EVENT_AMOUNT -> event.amount();
          case EVENT_TAX_AMOUNT -> event.taxAmount();
          case EVENT_NET_AMOUNT -> event.netAmount();
          case EVENT_COST_AMOUNT -> decimalAttribute(event, "costAmount");
          case CONSTANT -> line.constantAmount();
        };
    if (value == null || value.signum() <= 0) {
      throw new PostingException("Posting rule amount is missing or non-positive.");
    }
    return value;
  }

  private static BigDecimal decimalAttribute(final AccountingEvent event, final String key) {
    final String value = event.attributes().get(key);
    if (value == null || value.isBlank()) {
      throw new PostingException("Required event attribute is missing: " + key);
    }
    try {
      return new BigDecimal(value);
    } catch (NumberFormatException exception) {
      throw new PostingException("Event attribute is not a decimal: " + key);
    }
  }

  private static UUID mappedUuid(
      final AccountingEvent event, final PostingRuleLine line, final String mapping) {
    final String value = mappedValue(event, line, mapping);
    if (value == null) {
      return null;
    }
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException exception) {
      throw new PostingException("Invalid mapped " + mapping + " identifier.");
    }
  }

  private static String mappedValue(
      final AccountingEvent event, final PostingRuleLine line, final String mapping) {
    final String eventDimension = line.dimensionMappings().get(mapping);
    if (eventDimension == null) {
      return null;
    }
    final String value = event.dimensions().get(eventDimension);
    if (value == null || value.isBlank()) {
      throw new PostingException("Required event dimension is missing: " + eventDimension);
    }
    return value;
  }
}
