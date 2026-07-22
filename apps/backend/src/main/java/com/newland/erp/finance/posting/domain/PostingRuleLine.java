package com.newland.erp.finance.posting.domain;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record PostingRuleLine(
    UUID id,
    int lineNumber,
    Direction direction,
    AccountResolutionType accountResolutionType,
    UUID fixedAccountId,
    String accountAttributeKey,
    AmountExpression amountExpression,
    BigDecimal constantAmount,
    String descriptionTemplate,
    Map<String, String> dimensionMappings) {
  public PostingRuleLine {
    if (id == null
        || lineNumber < 1
        || direction == null
        || accountResolutionType == null
        || amountExpression == null) {
      throw new IllegalArgumentException("Posting rule line is invalid.");
    }
    if (accountResolutionType == AccountResolutionType.FIXED_ACCOUNT && fixedAccountId == null) {
      throw new PostingException("Fixed account resolution requires an account.");
    }
    if (accountResolutionType == AccountResolutionType.EVENT_ATTRIBUTE_ACCOUNT
        && (accountAttributeKey == null || accountAttributeKey.isBlank())) {
      throw new PostingException("Event account resolution requires an attribute key.");
    }
    if (amountExpression == AmountExpression.CONSTANT
        && (constantAmount == null || constantAmount.signum() < 0)) {
      throw new PostingException("Constant amount must be non-negative.");
    }
    dimensionMappings = dimensionMappings == null ? Map.of() : Map.copyOf(dimensionMappings);
  }

  public enum Direction {
    DEBIT,
    CREDIT
  }

  public enum AccountResolutionType {
    FIXED_ACCOUNT,
    EVENT_ATTRIBUTE_ACCOUNT
  }

  public enum AmountExpression {
    EVENT_AMOUNT,
    EVENT_TAX_AMOUNT,
    EVENT_NET_AMOUNT,
    EVENT_COST_AMOUNT,
    CONSTANT
  }
}
