package com.newland.erp.finance.posting.application;

import com.newland.erp.finance.posting.domain.PostingException;
import com.newland.erp.finance.posting.domain.PostingRule;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public final class PostingRuleService implements PostingRuleManagementPort {
  static final String MANAGE_CAPABILITY = "finance.posting.rule.manage";
  static final String ACTIVATE_CAPABILITY = "finance.posting.rule.activate";
  static final String RETIRE_CAPABILITY = "finance.posting.rule.retire";
  static final String READ_CAPABILITY = "finance.posting.rule.read";

  private final PostingPorts.RuleRepository rules;
  private final PostingPorts.AuthorizationPort authorization;
  private final PostingPorts.CurrentUserPort currentUsers;
  private final PostingPorts.AuditPort audit;
  private final PostingPorts.TransactionalOutboxPort outbox;
  private final Clock clock;

  public PostingRuleService(
      final PostingPorts.RuleRepository ruleRepository,
      final PostingPorts.AuthorizationPort authorizationPort,
      final PostingPorts.CurrentUserPort currentUserPort,
      final PostingPorts.AuditPort auditPort,
      final PostingPorts.TransactionalOutboxPort outboxPort,
      final Clock systemClock) {
    rules = ruleRepository;
    authorization = authorizationPort;
    currentUsers = currentUserPort;
    audit = auditPort;
    outbox = outboxPort;
    clock = systemClock;
  }

  @Override
  @Transactional
  public PostingRule create(final PostingRuleCommands.Create command) {
    final String actor = currentUsers.currentUser();
    requireScope(actor, MANAGE_CAPABILITY, command.companyId());
    final Instant now = Instant.now(clock);
    final PostingRule created =
        new PostingRule(
            UUID.randomUUID(),
            command.code(),
            command.name(),
            command.eventType(),
            command.companyId(),
            command.effectiveFrom(),
            command.effectiveTo(),
            command.priority(),
            PostingRule.Status.DRAFT,
            1,
            command.lines(),
            now,
            actor,
            null,
            null);
    rules.lockActivationScope(created);
    if (rules.findLatest(command.code(), command.companyId()).isPresent()) {
      throw new PostingException("A posting rule with this code and scope already exists.");
    }
    rules.save(created);
    recordLifecycle(actor, "POSTING_RULE_CREATED", "FinancePostingRuleCreated", created);
    return created;
  }

  @Override
  @Transactional
  public PostingRule createSuccessor(final PostingRuleCommands.CreateSuccessor command) {
    final PostingRule source = requireRule(command.sourceRuleId());
    final String actor = currentUsers.currentUser();
    requireScope(actor, MANAGE_CAPABILITY, source.companyId());
    rules.lockActivationScope(source);
    final PostingRule latest =
        rules
            .findLatest(source.code(), source.companyId())
            .orElseThrow(() -> new PostingException("Posting rule version history is missing."));
    if (!latest.postingRuleId().equals(source.postingRuleId())) {
      throw new PostingException("A successor can only be created from the latest rule version.");
    }
    final PostingRule successor =
        source.successor(
            UUID.randomUUID(),
            command.name(),
            command.effectiveFrom(),
            command.effectiveTo(),
            command.priority(),
            command.lines(),
            Instant.now(clock),
            actor);
    rules.save(successor);
    recordLifecycle(actor, "POSTING_RULE_VERSION_CREATED", "FinancePostingRuleVersionCreated", successor);
    return successor;
  }

  @Override
  @Transactional
  public PostingRule activate(final UUID postingRuleId) {
    final PostingRule draft = requireRule(postingRuleId);
    final String actor = currentUsers.currentUser();
    requireScope(actor, ACTIVATE_CAPABILITY, draft.companyId());
    rules.lockActivationScope(draft);
    if (!rules.findActivationConflicts(draft).isEmpty()) {
      throw new PostingException("Posting rule effective period conflicts with an active rule.");
    }
    final PostingRule activated = draft.activate(Instant.now(clock), actor);
    rules.transition(activated, PostingRule.Status.DRAFT);
    recordLifecycle(actor, "POSTING_RULE_ACTIVATED", "FinancePostingRuleActivated", activated);
    return activated;
  }

  @Override
  @Transactional
  public PostingRule retire(final UUID postingRuleId) {
    final PostingRule active = requireRule(postingRuleId);
    final String actor = currentUsers.currentUser();
    requireScope(actor, RETIRE_CAPABILITY, active.companyId());
    final PostingRule retired = active.retire(Instant.now(clock), actor);
    rules.transition(retired, PostingRule.Status.ACTIVE);
    recordLifecycle(actor, "POSTING_RULE_RETIRED", "FinancePostingRuleRetired", retired);
    return retired;
  }

  @Override
  @Transactional(readOnly = true)
  public PostingRule get(final UUID postingRuleId) {
    final PostingRule rule = requireRule(postingRuleId);
    requireScope(currentUsers.currentUser(), READ_CAPABILITY, rule.companyId());
    return rule;
  }

  @Override
  @Transactional(readOnly = true)
  public List<PostingRule> list(final UUID companyId) {
    requireScope(currentUsers.currentUser(), READ_CAPABILITY, companyId);
    return rules.list(companyId);
  }

  private PostingRule requireRule(final UUID postingRuleId) {
    return rules
        .findRule(postingRuleId)
        .orElseThrow(() -> new PostingException("Posting rule not found."));
  }

  private void requireScope(final String actor, final String capability, final UUID companyId) {
    if (companyId == null) {
      authorization.requireGlobal(actor, capability);
    } else {
      authorization.require(actor, capability, companyId);
    }
  }

  private void recordLifecycle(
      final String actor,
      final String auditEvent,
      final String domainEvent,
      final PostingRule rule) {
    audit.record(actor, auditEvent, rule.postingRuleId());
    outbox.publish(domainEvent, rule.postingRuleId());
  }
}
