package com.newland.erp.finance.posting.application;

import com.newland.erp.finance.posting.domain.PostingRuleLine;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class PostingRuleCommands {
  public record Create(
      String code,
      String name,
      String eventType,
      UUID companyId,
      LocalDate effectiveFrom,
      LocalDate effectiveTo,
      int priority,
      List<PostingRuleLine> lines) {}

  public record CreateSuccessor(
      UUID sourceRuleId,
      String name,
      LocalDate effectiveFrom,
      LocalDate effectiveTo,
      int priority,
      List<PostingRuleLine> lines) {}

  private PostingRuleCommands() {}
}
