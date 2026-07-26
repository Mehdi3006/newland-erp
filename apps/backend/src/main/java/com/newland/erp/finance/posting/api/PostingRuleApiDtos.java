package com.newland.erp.finance.posting.api;

import com.newland.erp.finance.posting.application.PostingRuleCommands;
import com.newland.erp.finance.posting.domain.PostingRule;
import com.newland.erp.finance.posting.domain.PostingRuleLine;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PostingRuleApiDtos {
  public record CreateRuleRequest(
      @NotBlank String code,
      @NotBlank String name,
      @NotBlank String eventType,
      UUID companyId,
      @NotNull LocalDate effectiveFrom,
      LocalDate effectiveTo,
      @PositiveOrZero int priority,
      @NotNull @Size(min = 2) List<@Valid RuleLineRequest> lines) {
    PostingRuleCommands.Create toCommand() {
      return new PostingRuleCommands.Create(
          code,
          name,
          eventType,
          companyId,
          effectiveFrom,
          effectiveTo,
          priority,
          lines.stream().map(RuleLineRequest::toDomain).toList());
    }
  }

  public record CreateSuccessorRequest(
      @NotBlank String name,
      @NotNull LocalDate effectiveFrom,
      LocalDate effectiveTo,
      @PositiveOrZero int priority,
      @NotNull @Size(min = 2) List<@Valid RuleLineRequest> lines) {
    PostingRuleCommands.CreateSuccessor toCommand(final UUID sourceRuleId) {
      return new PostingRuleCommands.CreateSuccessor(
          sourceRuleId,
          name,
          effectiveFrom,
          effectiveTo,
          priority,
          lines.stream().map(RuleLineRequest::toDomain).toList());
    }
  }

  public record RuleLineRequest(
      @Positive int lineNumber,
      @NotNull Direction direction,
      @NotNull AccountResolutionType accountResolutionType,
      UUID fixedAccountId,
      String accountAttributeKey,
      @NotNull AmountExpression amountExpression,
      BigDecimal constantAmount,
      String descriptionTemplate,
      Map<String, String> dimensionMappings) {
    PostingRuleLine toDomain() {
      return new PostingRuleLine(
          UUID.randomUUID(),
          lineNumber,
          PostingRuleLine.Direction.valueOf(direction.name()),
          PostingRuleLine.AccountResolutionType.valueOf(accountResolutionType.name()),
          fixedAccountId,
          accountAttributeKey,
          PostingRuleLine.AmountExpression.valueOf(amountExpression.name()),
          constantAmount,
          descriptionTemplate,
          dimensionMappings);
    }
  }

  public record PostingRuleResponse(
      UUID postingRuleId,
      String code,
      String name,
      String eventType,
      UUID companyId,
      LocalDate effectiveFrom,
      LocalDate effectiveTo,
      int priority,
      String status,
      int version,
      List<RuleLineResponse> lines,
      Instant createdAt,
      String createdBy,
      Instant updatedAt,
      String updatedBy) {
    static PostingRuleResponse from(final PostingRule rule) {
      return new PostingRuleResponse(
          rule.postingRuleId(),
          rule.code(),
          rule.name(),
          rule.eventType(),
          rule.companyId(),
          rule.effectiveFrom(),
          rule.effectiveTo(),
          rule.priority(),
          rule.status().name(),
          rule.version(),
          rule.lines().stream().map(RuleLineResponse::from).toList(),
          rule.createdAt(),
          rule.createdBy(),
          rule.updatedAt(),
          rule.updatedBy());
    }
  }

  public record RuleLineResponse(
      UUID id,
      int lineNumber,
      String direction,
      String accountResolutionType,
      UUID fixedAccountId,
      String accountAttributeKey,
      String amountExpression,
      BigDecimal constantAmount,
      String descriptionTemplate,
      Map<String, String> dimensionMappings) {
    static RuleLineResponse from(final PostingRuleLine line) {
      return new RuleLineResponse(
          line.id(),
          line.lineNumber(),
          line.direction().name(),
          line.accountResolutionType().name(),
          line.fixedAccountId(),
          line.accountAttributeKey(),
          line.amountExpression().name(),
          line.constantAmount(),
          line.descriptionTemplate(),
          line.dimensionMappings());
    }
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

  private PostingRuleApiDtos() {}
}
