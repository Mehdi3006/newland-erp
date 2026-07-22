package com.newland.erp.finance.posting.application;

import com.newland.erp.finance.posting.domain.PostingRule;
import java.util.List;
import java.util.UUID;

public interface PostingRuleManagementPort {
  PostingRule create(PostingRuleCommands.Create command);

  PostingRule createSuccessor(PostingRuleCommands.CreateSuccessor command);

  PostingRule activate(UUID postingRuleId);

  PostingRule retire(UUID postingRuleId);

  PostingRule get(UUID postingRuleId);

  List<PostingRule> list(UUID companyId);
}
