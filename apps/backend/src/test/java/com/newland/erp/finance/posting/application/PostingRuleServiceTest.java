package com.newland.erp.finance.posting.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.newland.erp.finance.posting.domain.PostingException;
import com.newland.erp.finance.posting.domain.PostingRule;
import com.newland.erp.finance.posting.domain.PostingRuleLine;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class PostingRuleServiceTest {
  private static final Instant NOW = Instant.parse("2026-07-22T00:00:00Z");
  private static final UUID COMPANY_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
  private final RuleStore rules = new RuleStore();
  private final RecordingAuthorization authorization = new RecordingAuthorization();
  private PostingRuleService service;

  @BeforeEach
  void setUp() {
    service =
        new PostingRuleService(
            rules,
            authorization,
            () -> "00000000-0000-0000-0000-000000000201",
            (actor, event, id) -> {},
            (event, id) -> {},
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void enforcesDraftActiveRetiredLifecycleAndCreatesImmutableSuccessor() {
    final PostingRule draft = service.create(create("STANDARD", COMPANY_ID, 10));
    final PostingRule active = service.activate(draft.postingRuleId());
    final PostingRule retired = service.retire(active.postingRuleId());
    final PostingRule successor =
        service.createSuccessor(
            new PostingRuleCommands.CreateSuccessor(
                retired.postingRuleId(),
                "Standard v2",
                LocalDate.parse("2027-01-01"),
                null,
                10,
                lines()));

    assertThat(active.status()).isEqualTo(PostingRule.Status.ACTIVE);
    assertThat(retired.status()).isEqualTo(PostingRule.Status.RETIRED);
    assertThat(successor.status()).isEqualTo(PostingRule.Status.DRAFT);
    assertThat(successor.version()).isEqualTo(2);
    assertThat(successor.code()).isEqualTo(retired.code());
    assertThat(rules.findRule(retired.postingRuleId())).contains(retired);
    assertThat(authorization.companyCapabilities)
        .contains(
            PostingRuleService.MANAGE_CAPABILITY,
            PostingRuleService.ACTIVATE_CAPABILITY,
            PostingRuleService.RETIRE_CAPABILITY);
  }

  @Test
  void rejectsAmbiguousOverlappingActiveRules() {
    final PostingRule first = service.create(create("FIRST", COMPANY_ID, 10));
    service.activate(first.postingRuleId());
    final PostingRule conflicting = service.create(create("SECOND", COMPANY_ID, 10));

    assertThatThrownBy(() -> service.activate(conflicting.postingRuleId()))
        .isInstanceOf(PostingException.class)
        .hasMessageContaining("conflicts");
    assertThat(rules.findRule(conflicting.postingRuleId()).orElseThrow().status())
        .isEqualTo(PostingRule.Status.DRAFT);

    final PostingRule higherPriority = service.create(create("HIGHER", COMPANY_ID, 20));
    assertThat(service.activate(higherPriority.postingRuleId()).status())
        .isEqualTo(PostingRule.Status.ACTIVE);
  }

  @Test
  void rejectsVersionForkAndUsesEnterpriseAuthorizationForGlobalRules() {
    final PostingRule global = service.create(create("GLOBAL", null, 1));
    final PostingRule active = service.activate(global.postingRuleId());
    final PostingRule retired = service.retire(active.postingRuleId());
    service.createSuccessor(
        new PostingRuleCommands.CreateSuccessor(
            retired.postingRuleId(),
            "Global v2",
            LocalDate.parse("2027-01-01"),
            null,
            1,
            globalLines()));

    assertThatThrownBy(
            () ->
                service.createSuccessor(
                    new PostingRuleCommands.CreateSuccessor(
                        retired.postingRuleId(),
                        "Fork",
                        LocalDate.parse("2028-01-01"),
                        null,
                        1,
                        globalLines())))
        .isInstanceOf(PostingException.class)
        .hasMessageContaining("latest");
    assertThat(authorization.globalCapabilities)
        .contains(
            PostingRuleService.MANAGE_CAPABILITY,
            PostingRuleService.ACTIVATE_CAPABILITY,
            PostingRuleService.RETIRE_CAPABILITY);
  }

  @Test
  void scopesGetAndListAuthorizationToRuleCompany() {
    final PostingRule rule = service.create(create("READ", COMPANY_ID, 3));

    assertThat(service.get(rule.postingRuleId())).isEqualTo(rule);
    assertThat(service.list(COMPANY_ID)).containsExactly(rule);
    assertThat(authorization.companyCapabilities)
        .contains(PostingRuleService.READ_CAPABILITY);
  }

  private PostingRuleCommands.Create create(
      final String code, final UUID companyId, final int priority) {
    return new PostingRuleCommands.Create(
        code,
        code + " Rule",
        "SALES_ORDER_APPROVED",
        companyId,
        LocalDate.parse("2026-01-01"),
        LocalDate.parse("2026-12-31"),
        priority,
        companyId == null ? globalLines() : lines());
  }

  private List<PostingRuleLine> lines() {
    return List.of(
        line(1, PostingRuleLine.Direction.DEBIT),
        line(2, PostingRuleLine.Direction.CREDIT));
  }

  private List<PostingRuleLine> globalLines() {
    return List.of(
        attributeLine(1, PostingRuleLine.Direction.DEBIT, "debitAccountId"),
        attributeLine(2, PostingRuleLine.Direction.CREDIT, "creditAccountId"));
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
        java.util.Map.of());
  }

  private PostingRuleLine attributeLine(
      final int lineNumber,
      final PostingRuleLine.Direction direction,
      final String attributeKey) {
    return new PostingRuleLine(
        UUID.randomUUID(),
        lineNumber,
        direction,
        PostingRuleLine.AccountResolutionType.EVENT_ATTRIBUTE_ACCOUNT,
        null,
        attributeKey,
        PostingRuleLine.AmountExpression.CONSTANT,
        BigDecimal.ONE,
        "line",
        java.util.Map.of());
  }

  private static final class RecordingAuthorization
      implements PostingPorts.AuthorizationPort {
    private final List<String> companyCapabilities = new ArrayList<>();
    private final List<String> globalCapabilities = new ArrayList<>();

    @Override
    public void require(final String actor, final String capability, final UUID companyId) {
      companyCapabilities.add(capability);
    }

    @Override
    public void requireGlobal(final String actor, final String capability) {
      globalCapabilities.add(capability);
    }
  }

  private static final class RuleStore implements PostingPorts.RuleRepository {
    private final HashMap<UUID, PostingRule> values = new HashMap<>();

    @Override
    public List<PostingRule> findApplicable(
        final String eventType, final UUID companyId, final LocalDate date) {
      return values.values().stream()
          .filter(rule -> rule.status() == PostingRule.Status.ACTIVE)
          .filter(rule -> rule.eventType().equals(eventType))
          .filter(rule -> rule.companyId() == null || rule.companyId().equals(companyId))
          .filter(rule -> !date.isBefore(rule.effectiveFrom()))
          .filter(rule -> rule.effectiveTo() == null || !date.isAfter(rule.effectiveTo()))
          .toList();
    }

    @Override
    public PostingRule save(final PostingRule rule) {
      if (values.putIfAbsent(rule.postingRuleId(), rule) != null) {
        throw new PostingException("Duplicate posting rule.");
      }
      return rule;
    }

    @Override
    public PostingRule transition(
        final PostingRule rule, final PostingRule.Status expectedStatus) {
      final PostingRule current = values.get(rule.postingRuleId());
      if (current == null || current.status() != expectedStatus) {
        throw new PostingException("Concurrent posting rule transition.");
      }
      values.put(rule.postingRuleId(), rule);
      return rule;
    }

    @Override
    public Optional<PostingRule> findRule(final UUID postingRuleId) {
      return Optional.ofNullable(values.get(postingRuleId));
    }

    @Override
    public Optional<PostingRule> findLatest(final String code, final UUID companyId) {
      return values.values().stream()
          .filter(rule -> rule.code().equals(code))
          .filter(rule -> Objects.equals(rule.companyId(), companyId))
          .max(Comparator.comparingInt(PostingRule::version));
    }

    @Override
    public void lockActivationScope(final PostingRule candidate) {}

    @Override
    public List<PostingRule> findActivationConflicts(final PostingRule candidate) {
      return values.values().stream()
          .filter(rule -> rule.status() == PostingRule.Status.ACTIVE)
          .filter(rule -> !rule.postingRuleId().equals(candidate.postingRuleId()))
          .filter(rule -> rule.conflictsWith(candidate))
          .toList();
    }

    @Override
    public List<PostingRule> list() {
      return List.copyOf(values.values());
    }

    @Override
    public List<PostingRule> list(final UUID companyId) {
      return values.values().stream()
          .filter(rule -> Objects.equals(rule.companyId(), companyId))
          .toList();
    }
  }
}
