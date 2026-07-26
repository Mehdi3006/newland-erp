package com.newland.erp.finance.posting.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newland.erp.finance.application.FinancePorts;
import com.newland.erp.finance.application.FinanceService;
import com.newland.erp.finance.domain.Account;
import com.newland.erp.finance.domain.AccountingPeriod;
import com.newland.erp.finance.domain.FiscalYear;
import com.newland.erp.finance.domain.FinanceException;
import com.newland.erp.finance.domain.JournalEntry;
import com.newland.erp.finance.infrastructure.JooqFinanceRepository;
import com.newland.erp.finance.posting.application.PostingPorts;
import com.newland.erp.finance.posting.application.PostingRuleEvaluator;
import com.newland.erp.finance.posting.domain.AccountingEvent;
import com.newland.erp.finance.posting.domain.PostingException;
import com.newland.erp.finance.posting.domain.PostingRequest;
import com.newland.erp.finance.posting.domain.PostingRule;
import com.newland.erp.finance.posting.domain.PostingRuleLine;
import com.newland.erp.platform.application.FileStoragePort;
import com.newland.erp.platform.application.PlatformService;
import com.newland.erp.platform.infrastructure.JooqPlatformRepository;
import com.newland.erp.identity.application.IdentityService;
import com.newland.erp.identity.application.PasswordHasher;
import com.newland.erp.identity.application.TokenService;
import com.newland.erp.identity.domain.Capability;
import com.newland.erp.identity.domain.EmailAddress;
import com.newland.erp.identity.domain.OrganizationScope;
import com.newland.erp.identity.domain.Permission;
import com.newland.erp.identity.domain.Role;
import com.newland.erp.identity.domain.RolePermissionAssignment;
import com.newland.erp.identity.domain.ScopeType;
import com.newland.erp.identity.domain.User;
import com.newland.erp.identity.domain.UserRoleAssignment;
import com.newland.erp.identity.domain.UserStatus;
import com.newland.erp.identity.domain.Username;
import com.newland.erp.identity.infrastructure.JooqIdentityRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.newland.erp.finance.posting.application.PostingService;
import com.newland.erp.finance.posting.domain.PostingResult;
import com.newland.erp.enterprise.application.integration.EnterpriseReferencePort;

@Testcontainers(disabledWithoutDocker = true)
final class JooqPostingRepositoryTest {
  private static final Instant NOW = Instant.parse("2026-07-22T00:00:00Z");
  private static final LocalDate POSTING_DATE = LocalDate.parse("2026-07-22");

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17-alpine");

  private DSLContext dsl;
  private ObjectMapper objectMapper;
  private JooqPostingRepository repository;

  @BeforeEach
  void setUp() {
    Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .cleanDisabled(false)
        .load()
        .clean();
    Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .load()
        .migrate();
    dsl = newDsl();
    objectMapper = new ObjectMapper();
    repository = new JooqPostingRepository(dsl, objectMapper);
  }

  @Test
  void persistsEventsRequestsAndRulesAcrossRepositoryInstances() {
    final UUID companyId = UUID.randomUUID();
    final UUID branchId = UUID.randomUUID();
    final UUID accountId = UUID.randomUUID();
    seedCompanyBranch(companyId, branchId);
    seedAccount(companyId, accountId);
    final AccountingEvent event = event(companyId, branchId, "persist-key");
    final PostingRequest request = request(event.eventId(), PostingRequest.Status.RECEIVED, 0, 0);
    final PostingRule rule = rule(companyId, accountId);

    repository.saveEvent(event);
    repository.saveRequest(request);
    saveActive(repository, rule);

    final JooqPostingRepository reloaded = new JooqPostingRepository(newDsl(), objectMapper);
    final AccountingEvent persisted = reloaded.findEvent(event.eventId()).orElseThrow();
    assertThat(persisted.eventId()).isEqualTo(event.eventId());
    assertThat(persisted.idempotencyKey()).isEqualTo(event.idempotencyKey());
    assertThat(persisted.amount()).isEqualByComparingTo(event.amount());
    assertThat(reloaded.findByIdempotencyKey(event.idempotencyKey()))
        .map(AccountingEvent::eventId)
        .contains(event.eventId());
    assertThat(reloaded.findRequest(request.postingRequestId())).contains(request);
    assertThat(reloaded.findRequestByEvent(event.eventId())).contains(request);
    assertThat(reloaded.findApplicable(event.eventType(), companyId, POSTING_DATE))
        .containsExactly(rule);
  }

  @Test
  void persistsRuleLifecycleAndGivesCompanyRulesPrecedenceOverGlobalRules() {
    final UUID companyId = UUID.randomUUID();
    final UUID branchId = UUID.randomUUID();
    final UUID accountId = UUID.randomUUID();
    seedCompanyBranch(companyId, branchId);
    seedAccount(companyId, accountId);
    final PostingRule global = globalRule();
    final PostingRule draft = draftRule(companyId, accountId);
    saveActive(repository, global);
    repository.save(draft);

    assertThat(repository.findApplicable(global.eventType(), companyId, POSTING_DATE))
        .containsExactly(global);

    final PostingRule active = draft.activate(NOW.plusSeconds(1), "reviewer");
    repository.transition(active, PostingRule.Status.DRAFT);
    assertThat(repository.findRule(active.postingRuleId())).contains(active);
    assertThat(repository.findApplicable(active.eventType(), companyId, POSTING_DATE))
        .containsExactly(active);

    final PostingRule retired = active.retire(NOW.plusSeconds(2), "reviewer");
    repository.transition(retired, PostingRule.Status.ACTIVE);
    assertThat(repository.findRule(retired.postingRuleId())).contains(retired);
    assertThat(repository.findApplicable(global.eventType(), companyId, POSTING_DATE))
        .containsExactly(global);
  }

  @Test
  void rejectsLineAppendAfterPostingRuleActivation() {
    final UUID companyId = UUID.randomUUID();
    final UUID branchId = UUID.randomUUID();
    final UUID accountId = UUID.randomUUID();
    seedCompanyBranch(companyId, branchId);
    seedAccount(companyId, accountId);
    final PostingRule draft = draftRule(companyId, accountId);
    repository.save(draft);
    repository.transition(draft.activate(NOW.plusSeconds(1), "reviewer"), PostingRule.Status.DRAFT);

    assertThatThrownBy(
            () ->
                dsl.insertInto(DSL.table("finance_posting_rule_line"))
                    .columns(
                        DSL.field("posting_rule_line_id"),
                        DSL.field("posting_rule_id"),
                        DSL.field("line_number"),
                        DSL.field("direction"),
                        DSL.field("account_resolution_type"),
                        DSL.field("fixed_account_id"),
                        DSL.field("amount_expression"),
                        DSL.field("description_template"),
                        DSL.field("dimension_mappings"))
                    .values(
                        UUID.randomUUID(),
                        draft.postingRuleId(),
                        3,
                        "DEBIT",
                        "FIXED_ACCOUNT",
                        accountId,
                        "EVENT_AMOUNT",
                        "late line",
                        JSONB.valueOf("{}"))
                    .execute())
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining("only be added to draft");
  }

  @Test
  void rejectsCrossCompanyFixedAccountAtPersistenceBoundary() {
    final UUID ruleCompanyId = UUID.randomUUID();
    final UUID accountCompanyId = UUID.randomUUID();
    seedCompanyBranch(ruleCompanyId, UUID.randomUUID());
    seedCompanyBranch(accountCompanyId, UUID.randomUUID());
    final UUID accountId = UUID.randomUUID();
    seedAccount(accountCompanyId, accountId);

    assertThatThrownBy(() -> repository.save(draftRule(ruleCompanyId, accountId)))
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining("outside rule company scope");
  }

  @Test
  void rejectsPostingRequestResolvedRuleVersionMismatch() {
    final UUID companyId = UUID.randomUUID();
    final UUID branchId = UUID.randomUUID();
    final UUID accountId = UUID.randomUUID();
    seedCompanyBranch(companyId, branchId);
    seedAccount(companyId, accountId);
    final PostingRule draft = draftRule(companyId, accountId);
    repository.save(draft);
    final AccountingEvent event = event(companyId, branchId, "rule-version-guard");
    repository.saveEvent(event);

    assertThatThrownBy(
            () ->
                dsl.insertInto(DSL.table("finance_posting_request"))
                    .columns(
                        DSL.field("posting_request_id"),
                        DSL.field("accounting_event_id"),
                        DSL.field("status"),
                        DSL.field("resolved_posting_rule_id"),
                        DSL.field("resolved_posting_rule_version"),
                        DSL.field("attempts"),
                        DSL.field("created_at"),
                        DSL.field("updated_at"),
                        DSL.field("version"))
                    .values(
                        UUID.randomUUID(),
                        event.eventId(),
                        "RULE_RESOLVED",
                        draft.postingRuleId(),
                        draft.version() + 1,
                        1,
                        OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC),
                        OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC),
                        1)
                    .execute())
        .isInstanceOf(DataAccessException.class);
  }

  @Test
  void rejectsAmbiguousOpenAccountingPeriodResolution() {
    final UUID companyId = UUID.randomUUID();
    seedCompanyBranch(companyId, UUID.randomUUID());
    final JooqFinanceRepository finance = new JooqFinanceRepository(dsl);
    for (int index = 1; index <= 2; index++) {
      final UUID fiscalYearId = UUID.randomUUID();
      finance.saveFiscalYear(
          new FiscalYear(
              fiscalYearId,
              companyId,
              "FY-AMBIGUOUS-" + index,
              POSTING_DATE.minusMonths(1),
              POSTING_DATE.plusMonths(1),
              false));
      finance.savePeriod(
          new AccountingPeriod(
              UUID.randomUUID(),
              fiscalYearId,
              "P-AMBIGUOUS-" + index,
              POSTING_DATE.minusDays(1),
              POSTING_DATE.plusDays(1),
              false));
    }

    assertThatThrownBy(() -> finance.findOpenPostingPeriod(companyId, POSTING_DATE))
        .isInstanceOf(FinanceException.class)
        .hasMessageContaining("multiple open accounting periods");
  }

  @Test
  void atomicallyAllowsOnlyOneConcurrentClaim() throws Exception {
    final AccountingEvent event = event(UUID.randomUUID(), "concurrent-claim");
    seedCompanyBranch(event.companyId(), event.branchId());
    final PostingRequest request = request(event.eventId(), PostingRequest.Status.RECEIVED, 0, 0);
    repository.saveEvent(event);
    repository.saveRequest(request);
    final int callers = 8;
    final CountDownLatch start = new CountDownLatch(1);
    final ExecutorService executor = Executors.newFixedThreadPool(callers);
    final List<Future<Boolean>> results = new ArrayList<>();
    try {
      for (int index = 0; index < callers; index++) {
        results.add(
            executor.submit(
                () -> {
                  start.await();
                  return new JooqPostingRepository(newDsl(), objectMapper)
                      .claimRequest(request.postingRequestId(), request.version())
                      .isPresent();
                }));
      }
      start.countDown();
      int claims = 0;
      for (Future<Boolean> result : results) {
        if (result.get()) {
          claims++;
        }
      }
      assertThat(claims).isEqualTo(1);
    } finally {
      executor.shutdownNow();
    }

    final PostingRequest claimed = repository.findRequest(request.postingRequestId()).orElseThrow();
    assertThat(claimed.status()).isEqualTo(PostingRequest.Status.VALIDATING);
    assertThat(claimed.attempts()).isEqualTo(1);
    assertThat(claimed.version()).isEqualTo(1);
  }

  @Test
  void concurrentRetriesCreateOnlyOneJournal() throws Exception {
    final UUID companyId = UUID.randomUUID();
    final UUID branchId = UUID.randomUUID();
    final UUID debitAccount = UUID.randomUUID();
    final UUID creditAccount = UUID.randomUUID();
    seedCompanyBranch(companyId, branchId);
    seedCurrency();
    final ServiceFixture fixture =
        serviceFixture(companyId, branchId, debitAccount, creditAccount, true);
    final PostingResult failed =
        fixture.service().submit(event(companyId, branchId, "concurrent-retry"));
    assertThat(failed.status()).isEqualTo(PostingRequest.Status.FAILED);

    final ExecutorService executor = Executors.newFixedThreadPool(2);
    final CountDownLatch start = new CountDownLatch(1);
    try {
      final List<Future<PostingResult>> retries =
          List.of(
              executor.submit(
                  () -> {
                    start.await();
                    return fixture.service().retry(failed.postingRequestId());
                  }),
              executor.submit(
                  () -> {
                    start.await();
                    return fixture.service().retry(failed.postingRequestId());
                  }));
      start.countDown();

      assertThat(retries.get(0).get().status()).isEqualTo(PostingRequest.Status.POSTED);
      assertThat(retries.get(1).get().status()).isEqualTo(PostingRequest.Status.POSTED);
      assertThat(count("finance_journal_entry")).isEqualTo(1);
      assertThat(count("finance_posting_request")).isEqualTo(1);
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void durablyReloadsAndClaimsFailedRequestForRetry() {
    final AccountingEvent event = event(UUID.randomUUID(), "durable-retry");
    seedCompanyBranch(event.companyId(), event.branchId());
    final PostingRequest failed = request(event.eventId(), PostingRequest.Status.FAILED, 2, 4);
    repository.saveEvent(event);
    repository.saveRequest(failed);

    final JooqPostingRepository afterRestart = new JooqPostingRepository(newDsl(), objectMapper);
    final PostingRequest reloaded = afterRestart.findRequest(failed.postingRequestId()).orElseThrow();
    final PostingRequest claimed =
        afterRestart
            .claimRequest(reloaded.postingRequestId(), reloaded.version())
            .orElseThrow();

    assertThat(claimed.status()).isEqualTo(PostingRequest.Status.VALIDATING);
    assertThat(claimed.attempts()).isEqualTo(3);
    assertThat(claimed.version()).isEqualTo(5);
  }

  @Test
  void rejectsStaleUpdatesAndProtectsPostedRequests() {
    final UUID companyId = UUID.randomUUID();
    final UUID branchId = UUID.randomUUID();
    seedCompanyBranch(companyId, branchId);
    seedCurrency();
    final ServiceFixture fixture =
        serviceFixture(companyId, branchId, UUID.randomUUID(), UUID.randomUUID());
    final PostingResult result =
        fixture.service().submit(event(companyId, branchId, "request-lock"));
    final PostingRequest posted =
        fixture.repository().findRequest(result.postingRequestId()).orElseThrow();

    assertThatThrownBy(() -> repository.updateRequest(posted))
        .isInstanceOf(PostingException.class);
    assertThatThrownBy(
            () ->
                dsl.update(DSL.table("finance_posting_request"))
                    .set(DSL.field("status", String.class), PostingRequest.Status.FAILED.name())
                    .where(
                        DSL.field("posting_request_id", UUID.class)
                            .eq(posted.postingRequestId()))
                    .execute())
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining("Posted posting requests are immutable");
  }

  @Test
  void enforcesEventIdempotencyAndAcceptedEventImmutability() {
    final AccountingEvent event = event(UUID.randomUUID(), "immutable-event");
    seedCompanyBranch(event.companyId(), event.branchId());
    repository.saveEvent(event);

    assertThatThrownBy(
            () -> repository.saveEvent(event(UUID.randomUUID(), event.idempotencyKey())))
        .isInstanceOf(DataAccessException.class);
    assertThatThrownBy(
            () ->
                dsl.update(DSL.table("finance_accounting_event"))
                    .set(DSL.field("description", String.class), "changed")
                    .where(DSL.field("event_id", UUID.class).eq(event.eventId()))
                    .execute())
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining("Accepted accounting events are immutable");
    assertThatThrownBy(
            () ->
                dsl.deleteFrom(DSL.table("finance_accounting_event"))
                    .where(DSL.field("event_id", UUID.class).eq(event.eventId()))
                    .execute())
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining("Accepted accounting events are immutable");
  }

  @Test
  void persistsAuditAndOutboxAndRollsThemBackWithPostingState() {
    final PostingInfrastructureAdapters.PlatformAdapter platform = platformAdapter(dsl);
    final UUID committedId = UUID.randomUUID();
    platform.record("architect", "POSTING_COMMITTED", committedId);
    platform.publish("PostingCommitted", committedId);
    assertThat(count("platform_audit_log")).isEqualTo(1);
    assertThat(count("platform_outbox")).isEqualTo(1);

    final AccountingEvent rolledBack = event(UUID.randomUUID(), "rollback-event");
    seedCompanyBranch(rolledBack.companyId(), rolledBack.branchId());
    assertThatThrownBy(
            () ->
                dsl.transaction(
                    configuration -> {
                      final DSLContext transactionDsl = DSL.using(configuration);
                      final JooqPostingRepository transactionRepository =
                          new JooqPostingRepository(transactionDsl, objectMapper);
                      final PostingInfrastructureAdapters.PlatformAdapter transactionPlatform =
                          platformAdapter(transactionDsl);
                      transactionRepository.saveEvent(rolledBack);
                      transactionRepository.saveRequest(
                          request(
                              rolledBack.eventId(), PostingRequest.Status.RECEIVED, 0, 0));
                      transactionPlatform.record(
                          "architect", "POSTING_ROLLED_BACK", rolledBack.eventId());
                      transactionPlatform.publish("PostingRolledBack", rolledBack.eventId());
                      throw new PostingException("forced rollback");
                    }))
        .isInstanceOf(PostingException.class)
        .hasMessage("forced rollback");

    assertThat(repository.findEvent(rolledBack.eventId())).isEmpty();
    assertThat(count("finance_posting_request")).isZero();
    assertThat(count("platform_audit_log")).isEqualTo(1);
    assertThat(count("platform_outbox")).isEqualTo(1);
  }

  @Test
  void createsAndPostsRealFinanceJournal() {
    final UUID companyId = UUID.randomUUID();
    final UUID branchId = UUID.randomUUID();
    final UUID debitAccount = UUID.randomUUID();
    final UUID creditAccount = UUID.randomUUID();
    final UUID fiscalYearId = UUID.randomUUID();
    final UUID periodId = UUID.randomUUID();
    seedCompanyBranch(companyId, branchId);
    final JooqFinanceRepository financeRepository = new JooqFinanceRepository(dsl);
    financeRepository.saveAccount(account(debitAccount, companyId, "1100"));
    financeRepository.saveAccount(account(creditAccount, companyId, "4100"));
    financeRepository.saveFiscalYear(
        new FiscalYear(
            fiscalYearId,
            companyId,
            "FY2026",
            LocalDate.parse("2026-01-01"),
            LocalDate.parse("2026-12-31"),
            false));
    financeRepository.savePeriod(
        new AccountingPeriod(
            periodId,
            fiscalYearId,
            "2026-07",
            LocalDate.parse("2026-07-01"),
            LocalDate.parse("2026-07-31"),
            false));
    final List<String> auditActions = new ArrayList<>();
    final List<String> outboxEvents = new ArrayList<>();
    final FinanceService finance =
        new FinanceService(
            financeRepository,
            (company, branch) -> {},
            currency -> {},
            allowAllAuthorization(),
            series -> "JE-POSTING-1",
            (actor, action, id) -> auditActions.add(action),
            (type, id) -> outboxEvents.add(type),
            (aggregate, attachment) -> {},
            Clock.fixed(NOW, ZoneOffset.UTC));
    final PostingInfrastructureAdapters.JournalAdapter adapter =
        new PostingInfrastructureAdapters.JournalAdapter(
            finance, financeRepository, postingRuleEvaluator());
    final AccountingEvent event = event(companyId, branchId, "real-journal");
    final PostingRule rule = rule(companyId, debitAccount, creditAccount);

    final PostingPorts.JournalReference reference = adapter.createAndPost(event, rule);

    final JournalEntry journal = financeRepository.findJournal(reference.journalEntryId()).orElseThrow();
    assertThat(reference.journalNumber()).isEqualTo("JE-POSTING-1");
    assertThat(journal.idempotencyKey()).isEqualTo("posting:" + event.eventId());
    assertThat(journal.status()).isEqualTo(JournalEntry.JournalStatus.POSTED);
    assertThat(journal.lines()).hasSize(2);
    assertThat(journal.lines())
        .extracting(
            JournalEntry.JournalLine::debit, JournalEntry.JournalLine::credit)
        .containsExactlyInAnyOrder(
            org.assertj.core.groups.Tuple.tuple(
                new BigDecimal("100.000000"), new BigDecimal("0.000000")),
            org.assertj.core.groups.Tuple.tuple(
                new BigDecimal("0.000000"), new BigDecimal("100.000000")));
    assertThat(auditActions)
        .containsExactly("FINANCE_JOURNAL_DRAFT_CREATED", "FINANCE_JOURNAL_POSTED");
    assertThat(outboxEvents).containsExactly("FinanceJournalPosted");
    assertThat(adapter.findReference(journal.id())).isEqualTo(reference);
    assertThatThrownBy(
            () ->
                dsl.insertInto(DSL.table("finance_journal_line"))
                    .columns(
                        DSL.field("id"),
                        DSL.field("journal_id"),
                        DSL.field("account_id"),
                        DSL.field("debit"),
                        DSL.field("credit"))
                    .values(
                        UUID.randomUUID(),
                        journal.id(),
                        debitAccount,
                        new BigDecimal("1.000000"),
                        BigDecimal.ZERO)
                    .execute())
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining("Posted journal lines are immutable");
  }

  @Test
  void concurrentSubmissionsCreateExactlyOneDurableJournal() throws Exception {
    final UUID companyId = UUID.randomUUID();
    final UUID branchId = UUID.randomUUID();
    final UUID debitAccount = UUID.randomUUID();
    final UUID creditAccount = UUID.randomUUID();
    seedCompanyBranch(companyId, branchId);
    seedCurrency();
    final ServiceFixture fixture =
        serviceFixture(companyId, branchId, debitAccount, creditAccount);
    final AccountingEvent event = event(companyId, branchId, "concurrent-service-submit");
    final int callers = 8;
    final CountDownLatch start = new CountDownLatch(1);
    final ExecutorService executor = Executors.newFixedThreadPool(callers);
    final List<Future<PostingResult>> results = new ArrayList<>();
    try {
      for (int index = 0; index < callers; index++) {
        results.add(
            executor.submit(
                () -> {
                  start.await();
                  return fixture.service().submit(event);
                }));
      }
      start.countDown();
      final var journalIds =
          results.stream()
              .map(
                  future -> {
                    try {
                      return future.get().journalEntryId();
                    } catch (Exception exception) {
                      throw new AssertionError(exception);
                    }
                  })
              .collect(Collectors.toSet());
      assertThat(journalIds).hasSize(1).doesNotContainNull();
    } finally {
      executor.shutdownNow();
    }

    assertThat(count("finance_accounting_event")).isEqualTo(1);
    assertThat(count("finance_posting_request")).isEqualTo(1);
    assertThat(count("finance_journal_entry")).isEqualTo(1);
    assertThat(count("finance_journal_line")).isEqualTo(2);
    assertThat(count("platform_audit_log")).isGreaterThanOrEqualTo(4);
    assertThat(count("platform_outbox")).isGreaterThanOrEqualTo(3);
  }

  @Test
  void rejectsAnIdempotencyKeyReusedWithAConflictingEventPayload() {
    final UUID companyId = UUID.randomUUID();
    final UUID branchId = UUID.randomUUID();
    final UUID debitAccount = UUID.randomUUID();
    final UUID creditAccount = UUID.randomUUID();
    seedCompanyBranch(companyId, branchId);
    seedCurrency();
    final ServiceFixture fixture =
        serviceFixture(companyId, branchId, debitAccount, creditAccount);
    final AccountingEvent accepted = event(companyId, branchId, "payload-safe-idempotency");

    final PostingResult posted = fixture.service().submit(accepted);
    final PostingResult replayed =
        fixture
            .service()
            .submit(
                copyEvent(
                    accepted,
                    accepted.eventId(),
                    accepted.companyId(),
                    new BigDecimal("100.000"),
                    accepted.occurredAt().plusSeconds(1)));

    assertThat(replayed.journalEntryId()).isEqualTo(posted.journalEntryId());
    assertThatThrownBy(
            () ->
                fixture
                    .service()
                    .submit(
                        copyEvent(
                            accepted,
                            UUID.randomUUID(),
                            accepted.companyId(),
                            accepted.amount(),
                            accepted.occurredAt())))
        .isInstanceOf(PostingException.class)
        .hasMessage("Idempotency key was reused with conflicting accounting event data.");
    assertThatThrownBy(
            () ->
                fixture
                    .service()
                    .submit(
                        copyEvent(
                            accepted,
                            accepted.eventId(),
                            UUID.randomUUID(),
                            accepted.amount(),
                            accepted.occurredAt())))
        .isInstanceOf(PostingException.class)
        .hasMessage("Idempotency key was reused with conflicting accounting event data.");
    assertThatThrownBy(
            () ->
                fixture
                    .service()
                    .submit(
                        copyEvent(
                            accepted,
                            accepted.eventId(),
                            accepted.companyId(),
                            accepted.amount().add(BigDecimal.ONE),
                            accepted.occurredAt())))
        .isInstanceOf(PostingException.class)
        .hasMessage("Idempotency key was reused with conflicting accounting event data.");

    assertThat(count("finance_accounting_event")).isEqualTo(1);
    assertThat(count("finance_posting_request")).isEqualTo(1);
    assertThat(count("finance_journal_entry")).isEqualTo(1);
  }

  @Test
  void resumesARequestStrandedInValidatingAfterRepositoryReload() {
    final UUID companyId = UUID.randomUUID();
    final UUID branchId = UUID.randomUUID();
    final UUID debitAccount = UUID.randomUUID();
    final UUID creditAccount = UUID.randomUUID();
    seedCompanyBranch(companyId, branchId);
    seedCurrency();
    final ServiceFixture fixture =
        serviceFixture(companyId, branchId, debitAccount, creditAccount);
    final AccountingEvent event = event(companyId, branchId, "stranded-validating");
    final PostingRequest validating =
        request(event.eventId(), PostingRequest.Status.VALIDATING, 1, 1);
    fixture.repository().saveEvent(event);
    fixture.repository().saveRequest(validating);

    final PostingResult result = fixture.service().retry(validating.postingRequestId());

    assertThat(result.status()).isEqualTo(PostingRequest.Status.POSTED);
    assertThat(result.journalEntryId()).isNotNull();
    assertThat(count("finance_journal_entry")).isEqualTo(1);
    assertThat(fixture.repository().findRequest(validating.postingRequestId()))
        .get()
        .extracting(PostingRequest::status)
        .isEqualTo(PostingRequest.Status.POSTED);
  }

  @Test
  void rollsBackJournalAuditAndOutboxThenPersistsRetryableFailure() {
    final UUID companyId = UUID.randomUUID();
    final UUID branchId = UUID.randomUUID();
    final UUID debitAccount = UUID.randomUUID();
    final UUID creditAccount = UUID.randomUUID();
    seedCompanyBranch(companyId, branchId);
    seedCurrency();
    final ServiceFixture fixture =
        serviceFixture(companyId, branchId, debitAccount, creditAccount, true);
    final AccountingEvent event = event(companyId, branchId, "rollback-full-posting");

    final PostingResult result = fixture.service().submit(event);

    assertThat(result.status()).isEqualTo(PostingRequest.Status.FAILED);
    assertThat(result.failureCode()).isEqualTo("POSTING_TECHNICAL_FAILURE");
    assertThat(result.failureMessage())
        .isEqualTo("Posting failed due to a temporary technical error.");
    assertThat(count("finance_journal_entry")).isZero();
    assertThat(count("finance_journal_line")).isZero();
    assertThat(fixture.repository().findRequest(result.postingRequestId()))
        .get()
        .extracting(PostingRequest::status)
        .isEqualTo(PostingRequest.Status.FAILED);
    assertThat(count("platform_audit_log")).isEqualTo(2);
    assertThat(count("platform_outbox")).isEqualTo(2);
  }

  @Test
  void persistsDeterministicRuleFailureAndKeepsRejectedRequestTerminal() {
    final UUID companyId = UUID.randomUUID();
    final UUID branchId = UUID.randomUUID();
    final UUID debitAccount = UUID.randomUUID();
    final UUID creditAccount = UUID.randomUUID();
    seedCompanyBranch(companyId, branchId);
    seedCurrency();
    final ServiceFixture fixture =
        serviceFixture(companyId, branchId, debitAccount, creditAccount, false, false);
    final AccountingEvent event = event(companyId, branchId, "deterministic-rejection");

    final PostingResult rejected = fixture.service().submit(event);
    final PostingResult retried = fixture.service().retry(rejected.postingRequestId());

    assertThat(rejected.status()).isEqualTo(PostingRequest.Status.REJECTED);
    assertThat(rejected.failureCode()).isEqualTo("POSTING_VALIDATION_FAILED");
    assertThat(rejected.failureMessage()).isEqualTo("No applicable posting rule exists.");
    assertThat(retried).isEqualTo(rejected);
    assertThat(fixture.repository().findRequest(rejected.postingRequestId()))
        .get()
        .satisfies(
            request -> {
              assertThat(request.status()).isEqualTo(PostingRequest.Status.REJECTED);
              assertThat(request.attempts()).isEqualTo(1);
            });
    assertThat(count("finance_journal_entry")).isZero();
  }

  @Test
  void enforcesAuthenticatedCapabilityAndCompanyScopeWithDurableIdentityData() {
    final UUID companyId = UUID.randomUUID();
    final UUID otherCompanyId = UUID.randomUUID();
    final UUID branchId = UUID.randomUUID();
    seedCompanyBranch(companyId, branchId);
    final UUID userId = UUID.randomUUID();
    final UUID roleId = UUID.randomUUID();
    final UUID permissionId = UUID.randomUUID();
    final JooqIdentityRepository identities = new JooqIdentityRepository(dsl);
    identities.insertUser(
        new User(
            userId,
            new Username("posting.user"),
            new EmailAddress("posting.user@example.com"),
            "Posting User",
            UserStatus.ACTIVE,
            0,
            null,
            NOW.plusSeconds(3600),
            NOW,
            NOW));
    identities.insertRole(new Role(roleId, "POSTER", "Poster", null, false));
    identities.insertPermission(
        new Permission(
            permissionId,
            new Capability("finance.posting.submit"),
            "Submit Finance posting events"));
    identities.insertUserRoleAssignment(
        new UserRoleAssignment(
            UUID.randomUUID(),
            userId,
            roleId,
            new OrganizationScope(ScopeType.COMPANY, companyId),
            NOW));
    identities.insertRolePermissionAssignment(
        new RolePermissionAssignment(UUID.randomUUID(), roleId, permissionId, NOW));
    final IdentityService identity =
        new IdentityService(
            identities,
            mock(PasswordHasher.class),
            mock(TokenService.class),
            event -> {},
            event -> {},
            Clock.fixed(NOW, ZoneOffset.UTC));
    final PostingInfrastructureAdapters.SecurityAdapter security =
        new PostingInfrastructureAdapters.SecurityAdapter(identity);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new TestingAuthenticationToken(userId.toString(), null, "ROLE_USER"));
    try {
      assertThat(security.currentUser()).isEqualTo(userId.toString());
      security.require(userId.toString(), "finance.posting.submit", companyId);
      assertThatThrownBy(
              () ->
                  security.require(
                      userId.toString(), "finance.posting.retry", companyId))
          .isInstanceOf(AccessDeniedException.class);
      assertThatThrownBy(
              () ->
                  security.require(
                      userId.toString(), "finance.posting.submit", otherCompanyId))
          .isInstanceOf(AccessDeniedException.class);
    } finally {
      SecurityContextHolder.clearContext();
    }
    assertThatThrownBy(security::currentUser)
        .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
  }

  @Test
  void postsJournalThroughAuthenticatedCompanyScopedIdentityIntegration() {
    final UUID companyId = UUID.randomUUID();
    final UUID branchId = UUID.randomUUID();
    final UUID debitAccount = UUID.randomUUID();
    final UUID creditAccount = UUID.randomUUID();
    seedCompanyBranch(companyId, branchId);
    seedCurrency();
    final SecurityFixture security =
        identitySecurity(companyId, "finance.posting.submit");
    final ServiceFixture fixture =
        serviceFixture(
            companyId,
            branchId,
            debitAccount,
            creditAccount,
            false,
            true,
            security.adapter(),
            security.adapter());
    try {
      final PostingResult result =
          fixture
              .service()
              .submit(
                  event(
                      companyId,
                      branchId,
                      "authenticated-full-posting",
                      security.userId().toString()));

      assertThat(result.status()).isEqualTo(PostingRequest.Status.POSTED);
      assertThat(result.journalEntryId()).isNotNull();
      assertThat(result.journalNumber()).startsWith("JE-");
      assertThat(count("finance_journal_entry")).isEqualTo(1);
      assertThat(count("finance_posting_request")).isEqualTo(1);
      assertThat(count("platform_audit_log")).isGreaterThanOrEqualTo(4);
      assertThat(count("platform_outbox")).isGreaterThanOrEqualTo(3);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  private PostingInfrastructureAdapters.PlatformAdapter platformAdapter(
      final DSLContext context) {
    final PlatformService platform =
        new PlatformService(
            new JooqPlatformRepository(context, objectMapper),
            event -> {},
            noOpStorage(),
            (jobId, jobType, scheduledAt, parameters) -> {},
            Clock.fixed(NOW, ZoneOffset.UTC));
    return new PostingInfrastructureAdapters.PlatformAdapter(platform);
  }

  private ServiceFixture serviceFixture(
      final UUID companyId,
      final UUID branchId,
      final UUID debitAccount,
      final UUID creditAccount) {
    return serviceFixture(companyId, branchId, debitAccount, creditAccount, false);
  }

  private ServiceFixture serviceFixture(
      final UUID companyId,
      final UUID branchId,
      final UUID debitAccount,
      final UUID creditAccount,
      final boolean failFirstJournalOutbox) {
    return serviceFixture(
        companyId,
        branchId,
        debitAccount,
        creditAccount,
        failFirstJournalOutbox,
        true);
  }

  private ServiceFixture serviceFixture(
      final UUID companyId,
      final UUID branchId,
      final UUID debitAccount,
      final UUID creditAccount,
      final boolean failFirstJournalOutbox,
      final boolean seedPostingRule) {
    final String actor = UUID.randomUUID().toString();
    return serviceFixture(
        companyId,
        branchId,
        debitAccount,
        creditAccount,
        failFirstJournalOutbox,
        seedPostingRule,
        allowPostingAuthorization(),
        () -> actor);
  }

  private ServiceFixture serviceFixture(
      final UUID companyId,
      final UUID branchId,
      final UUID debitAccount,
      final UUID creditAccount,
      final boolean failFirstJournalOutbox,
      final boolean seedPostingRule,
      final PostingPorts.AuthorizationPort authorization,
      final PostingPorts.CurrentUserPort currentUsers) {
    final DriverManagerDataSource source = new DriverManagerDataSource();
    source.setUrl(POSTGRES.getJdbcUrl());
    source.setUsername(POSTGRES.getUsername());
    source.setPassword(POSTGRES.getPassword());
    final DataSource transactionAware = new TransactionAwareDataSourceProxy(source);
    final DSLContext transactionalDsl = DSL.using(transactionAware, SQLDialect.POSTGRES);
    final DataSourceTransactionManager transactionManager =
        new DataSourceTransactionManager(source);
    final JooqFinanceRepository financeRepository =
        new JooqFinanceRepository(transactionalDsl);
    financeRepository.saveAccount(account(debitAccount, companyId, "1100"));
    financeRepository.saveAccount(account(creditAccount, companyId, "4100"));
    final UUID fiscalYearId = UUID.randomUUID();
    financeRepository.saveFiscalYear(
        new FiscalYear(
            fiscalYearId,
            companyId,
            "FY2026",
            LocalDate.parse("2026-01-01"),
            LocalDate.parse("2026-12-31"),
            false));
    financeRepository.savePeriod(
        new AccountingPeriod(
            UUID.randomUUID(),
            fiscalYearId,
            "2026-07",
            LocalDate.parse("2026-07-01"),
            LocalDate.parse("2026-07-31"),
            false));
    final JooqPostingRepository postingRepository =
        new JooqPostingRepository(transactionalDsl, objectMapper);
    if (seedPostingRule) {
      saveActive(postingRepository, rule(companyId, debitAccount, creditAccount));
    }
    final PlatformService platform =
        new PlatformService(
            new JooqPlatformRepository(transactionalDsl, objectMapper),
            event -> {},
            noOpStorage(),
            (jobId, jobType, scheduledAt, parameters) -> {},
            Clock.fixed(NOW, ZoneOffset.UTC));
    final PostingInfrastructureAdapters.PlatformAdapter platformAdapter =
        new PostingInfrastructureAdapters.PlatformAdapter(platform);
    final AtomicBoolean failOutbox = new AtomicBoolean(failFirstJournalOutbox);
    final PostingPorts.TransactionalOutboxPort outbox =
        (eventType, aggregateId) -> {
          if (eventType.equals("FinanceJournalPosted") && failOutbox.compareAndSet(true, false)) {
            throw new IllegalStateException("simulated outbox persistence failure");
          }
          platformAdapter.publish(eventType, aggregateId);
        };
    final PostingInfrastructureAdapters.FinanceReferenceAdapter references =
        new PostingInfrastructureAdapters.FinanceReferenceAdapter(financeRepository);
    final PostingRuleEvaluator evaluator =
        new PostingRuleEvaluator(
            references,
            references,
            references,
            new PostingInfrastructureAdapters.DimensionsAdapter(financeRepository));
    final FinanceService finance =
        new FinanceService(
            financeRepository,
            (company, branch) -> {},
            currency -> {},
            allowAllAuthorization(),
            series -> "JE-" + UUID.randomUUID(),
            platformAdapter::record,
            outbox::publish,
            (aggregate, attachment) -> {},
            Clock.fixed(NOW, ZoneOffset.UTC));
    final PostingService service =
        new PostingService(
            postingRepository,
            postingRepository,
            new PostingInfrastructureAdapters.CompanyAdapter(
                enterpriseReferences(transactionalDsl)),
            new PostingInfrastructureAdapters.BranchAdapter(
                enterpriseReferences(transactionalDsl)),
            new PostingInfrastructureAdapters.CurrencyAdapter(masterDataReferences(transactionalDsl)),
            new PostingInfrastructureAdapters.RateAdapter(
                masterDataReferences(transactionalDsl), enterpriseReferences(transactionalDsl)),
            new PostingInfrastructureAdapters.PeriodAdapter(financeRepository),
            new PostingInfrastructureAdapters.DimensionsAdapter(financeRepository),
            new PostingInfrastructureAdapters.JournalAdapter(finance, financeRepository, evaluator),
            platformAdapter,
            outbox,
            authorization,
            currentUsers,
            transactionManager,
            Clock.fixed(NOW, ZoneOffset.UTC));
    return new ServiceFixture(service, postingRepository);
  }

  private static PostingPorts.AuthorizationPort allowPostingAuthorization() {
    return new PostingPorts.AuthorizationPort() {
      @Override
      public void require(final String actor, final String capability, final UUID companyId) {}

      @Override
      public void requireGlobal(final String actor, final String capability) {}
    };
  }

  private static EnterpriseReferencePort enterpriseReferences(final DSLContext context) {
    return new EnterpriseReferencePort() {
      @Override
      public boolean isActiveCompany(final UUID companyId) {
        return context.fetchExists(
            DSL.table("company"),
            DSL.field("id", UUID.class)
                .eq(companyId)
                .and(DSL.field("status", String.class).eq("ACTIVE")));
      }

      @Override
      public boolean isActiveBranch(final UUID companyId, final UUID branchId) {
        return context.fetchExists(
            DSL.table("branch"),
            DSL.field("id", UUID.class)
                .eq(branchId)
                .and(DSL.field("company_id", UUID.class).eq(companyId))
                .and(DSL.field("status", String.class).eq("ACTIVE")));
      }

      @Override
      public java.util.Optional<String> companyBaseCurrency(final UUID companyId) {
        return context.select(DSL.field("base_currency", String.class))
            .from(DSL.table("company"))
            .where(DSL.field("id", UUID.class).eq(companyId)
                .and(DSL.field("status", String.class).eq("ACTIVE")))
            .fetchOptional(DSL.field("base_currency", String.class));
      }
    };
  }

  private static com.newland.erp.masterdata.application.integration.MasterDataReferencePort
      masterDataReferences(final DSLContext context) {
    return new com.newland.erp.masterdata.application.integration.MasterDataReferencePort() {
      @Override
      public boolean isActiveCurrency(final String code) {
        return context.fetchExists(
            DSL.table("master_data_record"),
            DSL.field("aggregate_type", String.class)
                .eq("CURRENCY")
                .and(DSL.field("code", String.class).eq(code))
                .and(DSL.field("active", Boolean.class).eq(true)));
      }

      @Override
      public java.util.Optional<ExchangeRateSnapshot> resolveExchangeRate(
          final UUID companyId, final String sourceCurrency, final String targetCurrency,
          final LocalDate effectiveDate) {
        return java.util.Optional.empty();
      }
    };
  }

  private void seedCurrency() {
    final OffsetDateTime now = OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC);
    dsl.insertInto(DSL.table("master_data_record"))
        .columns(
            DSL.field("id"),
            DSL.field("aggregate_type"),
            DSL.field("code"),
            DSL.field("display_name"),
            DSL.field("active"),
            DSL.field("attributes"),
            DSL.field("version"),
            DSL.field("created_at"),
            DSL.field("updated_at"))
        .values(
            UUID.randomUUID(),
            "CURRENCY",
            "USD",
            "US Dollar",
            true,
            JSONB.valueOf("{}"),
            0L,
            now,
            now)
        .execute();
  }

  private SecurityFixture identitySecurity(
      final UUID companyId, final String capability) {
    final UUID userId = UUID.randomUUID();
    final UUID roleId = UUID.randomUUID();
    final UUID permissionId = UUID.randomUUID();
    final JooqIdentityRepository identities = new JooqIdentityRepository(dsl);
    identities.insertUser(
        new User(
            userId,
            new Username("integrated.poster"),
            new EmailAddress("integrated.poster@example.com"),
            "Integrated Poster",
            UserStatus.ACTIVE,
            0,
            null,
            NOW.plusSeconds(3600),
            NOW,
            NOW));
    identities.insertRole(new Role(roleId, "INTEGRATED_POSTER", "Integrated Poster", null, false));
    identities.insertPermission(
        new Permission(permissionId, new Capability(capability), "Posting integration capability"));
    identities.insertUserRoleAssignment(
        new UserRoleAssignment(
            UUID.randomUUID(),
            userId,
            roleId,
            new OrganizationScope(ScopeType.COMPANY, companyId),
            NOW));
    identities.insertRolePermissionAssignment(
        new RolePermissionAssignment(UUID.randomUUID(), roleId, permissionId, NOW));
    final IdentityService identity =
        new IdentityService(
            identities,
            mock(PasswordHasher.class),
            mock(TokenService.class),
            event -> {},
            event -> {},
            Clock.fixed(NOW, ZoneOffset.UTC));
    final PostingInfrastructureAdapters.SecurityAdapter adapter =
        new PostingInfrastructureAdapters.SecurityAdapter(identity);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new TestingAuthenticationToken(userId.toString(), null, "ROLE_USER"));
    return new SecurityFixture(userId, adapter);
  }

  private record ServiceFixture(
      PostingService service, JooqPostingRepository repository) {}

  private record SecurityFixture(
      UUID userId, PostingInfrastructureAdapters.SecurityAdapter adapter) {}

  private static FileStoragePort noOpStorage() {
    return new FileStoragePort() {
      @Override
      public String put(final String storageKey, final byte[] content) {
        return storageKey;
      }

      @Override
      public byte[] get(final String storageKey) {
        return new byte[0];
      }
    };
  }

  private static FinancePorts.AuthorizationPort allowAllAuthorization() {
    return new FinancePorts.AuthorizationPort() {
      @Override
      public void require(final String actor, final String capability, final UUID companyId) {}

      @Override
      public void requireCostCenter(final String actor, final UUID costCenterId) {}

      @Override
      public void requireDimension(final String actor, final String dimensionCode) {}
    };
  }

  private static PostingRuleEvaluator postingRuleEvaluator() {
    final PostingPorts.AccountResolutionPort accounts =
        new PostingPorts.AccountResolutionPort() {
          @Override
          public void requireAccount(final UUID companyId, final UUID accountId) {}

          @Override
          public UUID resolveAttribute(
              final UUID companyId,
              final String key,
              final Map<String, String> attributes) {
            return UUID.fromString(attributes.get(key));
          }
        };
    final PostingPorts.FinancialDimensionValidationPort dimensions =
        new PostingPorts.FinancialDimensionValidationPort() {
          @Override
          public void requireDimensions(
              final UUID companyId, final Map<String, String> values) {}

          @Override
          public void requireDimension(final UUID companyId, final String dimensionCode) {}
        };
    return new PostingRuleEvaluator(
        accounts, (companyId, costCenterId) -> {}, (companyId, profitCenterId) -> {}, dimensions);
  }

  private DSLContext newDsl() {
    return DSL.using(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
  }

  private int count(final String table) {
    return dsl.fetchCount(DSL.table(table));
  }

  private void seedAccount(final UUID companyId, final UUID accountId) {
    new JooqFinanceRepository(dsl).saveAccount(account(accountId, companyId, "1000"));
  }

  private void seedCompanyBranch(final UUID companyId, final UUID branchId) {
    final UUID enterpriseId = UUID.randomUUID();
    final UUID legalEntityId = UUID.randomUUID();
    final OffsetDateTime now = OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC);
    dsl.insertInto(DSL.table("enterprise"))
        .columns(
            DSL.field("id"),
            DSL.field("code"),
            DSL.field("name"),
            DSL.field("localized_name"),
            DSL.field("status"),
            DSL.field("created_at"),
            DSL.field("created_by"),
            DSL.field("updated_at"),
            DSL.field("updated_by"),
            DSL.field("version"))
        .values(
            enterpriseId,
            "ENT-" + companyId.toString().substring(0, 8),
            "Enterprise",
            JSONB.valueOf("{}"),
            "ACTIVE",
            now,
            "test",
            now,
            "test",
            0L)
        .execute();
    dsl.insertInto(DSL.table("legal_entity"))
        .columns(
            DSL.field("id"),
            DSL.field("enterprise_id"),
            DSL.field("code"),
            DSL.field("name"),
            DSL.field("localized_name"),
            DSL.field("country_code"),
            DSL.field("base_currency"),
            DSL.field("status"),
            DSL.field("created_at"),
            DSL.field("created_by"),
            DSL.field("updated_at"),
            DSL.field("updated_by"),
            DSL.field("version"))
        .values(
            legalEntityId,
            enterpriseId,
            "LE",
            "Legal Entity",
            JSONB.valueOf("{}"),
            "US",
            "USD",
            "ACTIVE",
            now,
            "test",
            now,
            "test",
            0L)
        .execute();
    dsl.insertInto(DSL.table("company"))
        .columns(
            DSL.field("id"),
            DSL.field("enterprise_id"),
            DSL.field("legal_entity_id"),
            DSL.field("code"),
            DSL.field("name"),
            DSL.field("localized_name"),
            DSL.field("country_code"),
            DSL.field("base_currency"),
            DSL.field("time_zone_id"),
            DSL.field("status"),
            DSL.field("created_at"),
            DSL.field("created_by"),
            DSL.field("updated_at"),
            DSL.field("updated_by"),
            DSL.field("version"))
        .values(
            companyId,
            enterpriseId,
            legalEntityId,
            "COMP",
            "Company",
            JSONB.valueOf("{}"),
            "US",
            "USD",
            "UTC",
            "ACTIVE",
            now,
            "test",
            now,
            "test",
            0L)
        .execute();
    dsl.insertInto(DSL.table("branch"))
        .columns(
            DSL.field("id"),
            DSL.field("enterprise_id"),
            DSL.field("company_id"),
            DSL.field("code"),
            DSL.field("name"),
            DSL.field("localized_name"),
            DSL.field("status"),
            DSL.field("created_at"),
            DSL.field("created_by"),
            DSL.field("updated_at"),
            DSL.field("updated_by"),
            DSL.field("version"))
        .values(
            branchId,
            enterpriseId,
            companyId,
            "BR",
            "Branch",
            JSONB.valueOf("{}"),
            "ACTIVE",
            now,
            "test",
            now,
            "test",
            0L)
        .execute();
  }

  private static Account account(final UUID id, final UUID companyId, final String code) {
    return new Account(
        id, companyId, code, "Posting Account " + code, Account.AccountType.ASSET, null, true, true);
  }

  private static AccountingEvent event(final UUID companyId, final String idempotencyKey) {
    return event(companyId, UUID.randomUUID(), idempotencyKey);
  }

  private static AccountingEvent event(
      final UUID companyId, final UUID branchId, final String idempotencyKey) {
    return event(companyId, branchId, idempotencyKey, "architect");
  }

  private static AccountingEvent event(
      final UUID companyId,
      final UUID branchId,
      final String idempotencyKey,
      final String actor) {
    return new AccountingEvent(
        UUID.randomUUID(),
        idempotencyKey,
        "SALES_ORDER_APPROVED",
        "sales",
        "SALES_ORDER",
        UUID.randomUUID(),
        "SO-100",
        companyId,
        branchId,
        POSTING_DATE,
        POSTING_DATE,
        "USD",
        BigDecimal.ONE,
        new BigDecimal("100.00"),
        new BigDecimal("10.00"),
        new BigDecimal("90.00"),
        "Posting event",
        Map.of(),
        Map.of(),
        NOW,
        actor,
        1);
  }

  private static AccountingEvent copyEvent(
      final AccountingEvent source,
      final UUID eventId,
      final UUID companyId,
      final BigDecimal amount,
      final Instant occurredAt) {
    return new AccountingEvent(
        eventId,
        source.idempotencyKey(),
        source.eventType(),
        source.sourceModule(),
        source.sourceDocumentType(),
        source.sourceDocumentId(),
        source.sourceDocumentNumber(),
        companyId,
        source.branchId(),
        source.eventDate(),
        source.accountingDate(),
        source.currencyCode(),
        source.exchangeRate(),
        amount,
        source.taxAmount(),
        source.netAmount(),
        source.description(),
        source.dimensions(),
        source.attributes(),
        occurredAt,
        source.submittedBy(),
        source.version());
  }

  private static PostingRequest request(
      final UUID eventId,
      final PostingRequest.Status status,
      final int attempts,
      final int version) {
    return new PostingRequest(
        UUID.randomUUID(),
        eventId,
        status,
        null,
        null,
        null,
        status == PostingRequest.Status.FAILED ? "TECHNICAL_FAILURE" : null,
        status == PostingRequest.Status.FAILED ? "retryable" : null,
        attempts,
        NOW,
        NOW,
        version);
  }

  private static PostingRequest transition(
      final PostingRequest current, final PostingRequest.Status status, final int version) {
    return new PostingRequest(
        current.postingRequestId(),
        current.accountingEventId(),
        status,
        current.resolvedPostingRuleId(),
        current.resolvedPostingRuleVersion(),
        current.journalEntryId(),
        current.failureCode(),
        current.failureMessage(),
        current.attempts(),
        current.createdAt(),
        NOW.plusSeconds(version),
        version);
  }

  private static PostingRule rule(final UUID companyId, final UUID accountId) {
    return rule(companyId, accountId, accountId);
  }

  private static void saveActive(
      final JooqPostingRepository target, final PostingRule activeRule) {
    final PostingRule draft =
        new PostingRule(
            activeRule.postingRuleId(),
            activeRule.code(),
            activeRule.name(),
            activeRule.eventType(),
            activeRule.companyId(),
            activeRule.effectiveFrom(),
            activeRule.effectiveTo(),
            activeRule.priority(),
            PostingRule.Status.DRAFT,
            activeRule.version(),
            activeRule.lines(),
            activeRule.createdAt(),
            activeRule.createdBy(),
            null,
            null);
    target.save(draft);
    target.transition(activeRule, PostingRule.Status.DRAFT);
  }

  private static PostingRule rule(
      final UUID companyId, final UUID debitAccount, final UUID creditAccount) {
    return new PostingRule(
        UUID.randomUUID(),
        "SALES-POSTING",
        "Sales posting",
        "SALES_ORDER_APPROVED",
        companyId,
        LocalDate.parse("2026-01-01"),
        null,
        100,
        PostingRule.Status.ACTIVE,
        1,
        List.of(
            line(1, PostingRuleLine.Direction.DEBIT, debitAccount),
            line(2, PostingRuleLine.Direction.CREDIT, creditAccount)),
        NOW,
        "architect",
        NOW,
        "architect");
  }

  private static PostingRule globalRule() {
    return new PostingRule(
        UUID.randomUUID(),
        "GLOBAL-SALES-POSTING",
        "Global sales posting",
        "SALES_ORDER_APPROVED",
        null,
        LocalDate.parse("2026-01-01"),
        null,
        1,
        PostingRule.Status.ACTIVE,
        1,
        List.of(
            attributeLine(1, PostingRuleLine.Direction.DEBIT, "debitAccountId"),
            attributeLine(2, PostingRuleLine.Direction.CREDIT, "creditAccountId")),
        NOW,
        "architect",
        NOW,
        "architect");
  }

  private static PostingRule draftRule(final UUID companyId, final UUID accountId) {
    return new PostingRule(
        UUID.randomUUID(),
        "COMPANY-SALES-POSTING",
        "Company sales posting",
        "SALES_ORDER_APPROVED",
        companyId,
        LocalDate.parse("2026-01-01"),
        null,
        1,
        PostingRule.Status.DRAFT,
        1,
        List.of(
            line(1, PostingRuleLine.Direction.DEBIT, accountId),
            line(2, PostingRuleLine.Direction.CREDIT, accountId)),
        NOW,
        "architect",
        null,
        null);
  }

  private static PostingRuleLine line(
      final int number, final PostingRuleLine.Direction direction, final UUID accountId) {
    return new PostingRuleLine(
        UUID.randomUUID(),
        number,
        direction,
        PostingRuleLine.AccountResolutionType.FIXED_ACCOUNT,
        accountId,
        null,
        PostingRuleLine.AmountExpression.EVENT_AMOUNT,
        null,
        "Posting line",
        Map.of());
  }

  private static PostingRuleLine attributeLine(
      final int number, final PostingRuleLine.Direction direction, final String attributeKey) {
    return new PostingRuleLine(
        UUID.randomUUID(),
        number,
        direction,
        PostingRuleLine.AccountResolutionType.EVENT_ATTRIBUTE_ACCOUNT,
        null,
        attributeKey,
        PostingRuleLine.AmountExpression.EVENT_AMOUNT,
        null,
        "Posting line",
        Map.of());
  }
}
