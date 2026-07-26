package com.newland.erp.finance.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newland.erp.enterprise.application.integration.EnterpriseReferencePort;
import com.newland.erp.finance.application.FinanceCommands;
import com.newland.erp.finance.application.FinancePorts;
import com.newland.erp.finance.application.FinanceService;
import com.newland.erp.finance.domain.Account;
import com.newland.erp.finance.domain.AccountingPeriod;
import com.newland.erp.finance.domain.AccountingPeriodContract;
import com.newland.erp.finance.domain.FinanceException;
import com.newland.erp.finance.domain.FiscalYear;
import com.newland.erp.finance.domain.FinancialDocumentNumber;
import com.newland.erp.finance.domain.JournalEntry;
import com.newland.erp.finance.domain.JournalPostingSnapshot;
import com.newland.erp.masterdata.application.integration.MasterDataReferencePort;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
final class JooqGeneralLedgerCoreTest {
  private static final Instant NOW = Instant.parse("2026-07-26T00:00:00Z");

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17-alpine");

  private DSLContext dsl;
  private JooqFinanceRepository repository;
  private UUID companyId;
  private UUID branchId;
  private UUID fiscalYearId;
  private UUID periodId;
  private UUID debitAccountId;
  private UUID creditAccountId;

  @BeforeEach
  void setUp() {
    final Flyway flyway =
        Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .locations("classpath:db/migration")
            .cleanDisabled(false)
            .load();
    flyway.clean();
    flyway.migrate();
    dsl = DSL.using(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    repository = new JooqFinanceRepository(dsl, new ObjectMapper());
    companyId = UUID.randomUUID();
    branchId = UUID.randomUUID();
    fiscalYearId = UUID.randomUUID();
    periodId = UUID.randomUUID();
    debitAccountId = UUID.randomUUID();
    creditAccountId = UUID.randomUUID();
    seedCompany();
    repository.saveAccount(account(debitAccountId, "1100"));
    repository.saveAccount(account(creditAccountId, "4100"));
    repository.saveFiscalYear(
        new FiscalYear(
            fiscalYearId,
            companyId,
            "FY2026",
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 12, 31),
            false));
    repository.savePeriod(
        new AccountingPeriod(
            periodId,
            fiscalYearId,
            "2026-07",
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 31),
            AccountingPeriod.State.OPEN));
  }

  @Test
  void persistsPeriodStateAndExcludesClosingPeriodsFromOrdinaryResolution() {
    final AccountingPeriod open = repository.findPeriod(periodId).orElseThrow();
    final AccountingPeriod closing = open.transitionTo(AccountingPeriod.State.CLOSING);
    repository.updatePeriod(closing, AccountingPeriod.State.OPEN);

    assertThat(repository.findPeriod(periodId)).contains(closing);
    assertThat(repository.findOpenPostingPeriod(companyId, LocalDate.of(2026, 7, 15))).isEmpty();
  }

  @Test
  void persistsPostingSnapshotAndProtectsPostedJournalAndSnapshot() {
    final JournalEntry draft = repository.insertJournal(journal("snapshot-key"));
    final JournalPostingSnapshot snapshot =
        snapshot(draft.id());
    repository.savePostingSnapshot(snapshot);
    repository.saveJournal(draft.post());

    assertThat(repository.findPostingSnapshot(draft.id())).contains(snapshot);
    assertThatThrownBy(
            () ->
                dsl.update(DSL.table("finance_journal_entry"))
                    .set(DSL.field("actor"), "tampered")
                    .where(DSL.field("id", UUID.class).eq(draft.id()))
                    .execute())
        .isInstanceOf(DataAccessException.class);
    assertThatThrownBy(
            () ->
                dsl.deleteFrom(DSL.table("finance_journal_posting_snapshot"))
                    .where(DSL.field("journal_entry_id", UUID.class).eq(draft.id()))
                    .execute())
        .isInstanceOf(DataAccessException.class);
  }

  @Test
  void databaseRejectsEveryPostedJournalWithoutSnapshot() {
    final JournalEntry draft = repository.insertJournal(journal("missing-snapshot"));

    assertThatThrownBy(() -> repository.saveJournal(draft.post()))
        .isInstanceOf(DataAccessException.class)
        .rootCause()
        .hasMessageContaining("requires an authoritative posting snapshot");
  }

  @Test
  void resolvesClosingPeriodsOnlyForCloseAdjustmentsAndNeverClosedPeriods() {
    final LocalDate date = LocalDate.of(2026, 7, 15);
    final AccountingPeriod open = repository.findPeriod(periodId).orElseThrow();
    repository.updatePeriod(open.transitionTo(AccountingPeriod.State.CLOSING), open.state());

    assertThat(
            repository.findPostingPeriod(
                companyId,
                date,
                com.newland.erp.finance.domain.AccountingPeriodContract.PostingPurpose.ORDINARY))
        .isEmpty();
    assertThat(
            repository.findPostingPeriod(
                companyId,
                date,
                com.newland.erp.finance.domain.AccountingPeriodContract.PostingPurpose
                    .CLOSE_ADJUSTMENT))
        .contains(new com.newland.erp.finance.application.FinanceRepository.PostingPeriod(
            fiscalYearId, periodId));

    final AccountingPeriod closing = repository.findPeriod(periodId).orElseThrow();
    repository.updatePeriod(closing.transitionTo(AccountingPeriod.State.CLOSED), closing.state());
    assertThat(
            repository.findPostingPeriod(
                companyId,
                date,
                com.newland.erp.finance.domain.AccountingPeriodContract.PostingPurpose
                    .CLOSE_ADJUSTMENT))
        .isEmpty();
  }

  @Test
  void postsClosingAdjustmentWithMandatorySnapshotAndRejectsOrdinaryPosting() {
    final JournalEntry draft = repository.insertJournal(journal("closing-adjustment"));
    final AccountingPeriod open = repository.findPeriod(periodId).orElseThrow();
    repository.updatePeriod(open.transitionTo(AccountingPeriod.State.CLOSING), open.state());
    final FinanceService service = service();

    assertThatThrownBy(
            () ->
                service.postJournal(
                    new FinanceCommands.PostJournal(
                        draft.id(), AccountingPeriodContract.PostingPurpose.ORDINARY, Map.of(), "actor")))
        .isInstanceOf(FinanceException.class)
        .hasMessageContaining("does not allow");

    final JournalEntry posted =
        service.postJournal(
            new FinanceCommands.PostJournal(
                draft.id(),
                AccountingPeriodContract.PostingPurpose.CLOSE_ADJUSTMENT,
                Map.of("taxCategory", "NONE"),
                "actor"));

    assertThat(posted.status()).isEqualTo(JournalEntry.JournalStatus.POSTED);
    assertThat(repository.findPostingSnapshot(posted.id()))
        .get()
        .extracting(JournalPostingSnapshot::postedAt)
        .isEqualTo(NOW);
  }

  @Test
  void reversesIntoPeriodResolvedFromReversalDateAfterOriginalPeriodCloses() {
    final JournalEntry originalDraft = repository.insertJournal(journal("original-for-reversal"));
    repository.savePostingSnapshot(snapshot(originalDraft.id()));
    final JournalEntry original = repository.saveJournal(originalDraft.post());
    final AccountingPeriod open = repository.findPeriod(periodId).orElseThrow();
    final AccountingPeriod closing =
        repository.updatePeriod(open.transitionTo(AccountingPeriod.State.CLOSING), open.state());
    repository.updatePeriod(
        closing.transitionTo(AccountingPeriod.State.CLOSED), AccountingPeriod.State.CLOSING);
    final UUID augustPeriodId = UUID.randomUUID();
    repository.savePeriod(
        new AccountingPeriod(
            augustPeriodId,
            fiscalYearId,
            "2026-08",
            LocalDate.of(2026, 8, 1),
            LocalDate.of(2026, 8, 31),
            AccountingPeriod.State.OPEN));

    final JournalEntry reversal =
        service()
            .reverseJournal(
                new FinanceCommands.ReverseJournal(
                    original.id(),
                    "reversal-" + UUID.randomUUID(),
                    LocalDate.of(2026, 8, 5),
                    AccountingPeriodContract.PostingPurpose.ORDINARY,
                    "actor"));

    assertThat(reversal.status()).isEqualTo(JournalEntry.JournalStatus.POSTED);
    assertThat(reversal.periodId()).isEqualTo(augustPeriodId);
    assertThat(reversal.postingDate()).isEqualTo(LocalDate.of(2026, 8, 5));
    assertThat(reversal.reversalOfId()).isEqualTo(original.id());
    assertThat(repository.findPostingSnapshot(reversal.id())).isPresent();
  }

  @Test
  void rejectsInactiveAndForeignCompanyCostAndProfitCenters() {
    final UUID inactiveCostCenter = UUID.randomUUID();
    final UUID inactiveProfitCenter = UUID.randomUUID();
    dsl.insertInto(DSL.table("finance_cost_center"))
        .columns(
            DSL.field("id"),
            DSL.field("company_id"),
            DSL.field("code"),
            DSL.field("active"))
        .values(inactiveCostCenter, companyId, "CC-INACTIVE", false)
        .execute();
    dsl.insertInto(DSL.table("finance_profit_center"))
        .columns(
            DSL.field("id"),
            DSL.field("company_id"),
            DSL.field("code"),
            DSL.field("active"))
        .values(inactiveProfitCenter, companyId, "PC-INACTIVE", false)
        .execute();
    final BigDecimal amount = new BigDecimal("100.000000");

    assertThatThrownBy(
            () ->
                service()
                    .createJournal(
                        createJournal(
                            "inactive-cost",
                            line(
                                debitAccountId,
                                amount,
                                BigDecimal.ZERO,
                                inactiveCostCenter,
                                null),
                            line(creditAccountId, BigDecimal.ZERO, amount))))
        .isInstanceOf(FinanceException.class)
        .hasMessageContaining("Cost center is inactive");
    assertThatThrownBy(
            () ->
                service()
                    .createJournal(
                        createJournal(
                            "inactive-profit",
                            line(
                                debitAccountId,
                                amount,
                                BigDecimal.ZERO,
                                null,
                                inactiveProfitCenter),
                            line(creditAccountId, BigDecimal.ZERO, amount))))
        .isInstanceOf(FinanceException.class)
        .hasMessageContaining("Profit center is inactive");
    assertThatThrownBy(
            () ->
                service()
                    .createJournal(
                        createJournal(
                            "foreign-center",
                            line(
                                debitAccountId,
                                amount,
                                BigDecimal.ZERO,
                                UUID.randomUUID(),
                                null),
                            line(creditAccountId, BigDecimal.ZERO, amount))))
        .isInstanceOf(FinanceException.class)
        .hasMessageContaining("outside journal company scope");
  }

  @Test
  void persistsReconciledAuthoritativeForeignCurrencySnapshot() {
    final UUID euroId = UUID.randomUUID();
    seedCurrency(euroId, "EUR");
    final BigDecimal baseAmount = new BigDecimal("110.000000");
    final BigDecimal transactionAmount = new BigDecimal("100.000000");
    final BigDecimal rate = new BigDecimal("1.100000000000");
    final JournalEntry journal =
        new JournalEntry(
            UUID.randomUUID(),
            "JE-" + UUID.randomUUID(),
            "foreign-" + UUID.randomUUID(),
            companyId,
            branchId,
            fiscalYearId,
            periodId,
            LocalDate.of(2026, 7, 15),
            JournalEntry.JournalStatus.DRAFT,
            List.of(
                foreignLine(
                    debitAccountId,
                    baseAmount,
                    BigDecimal.ZERO,
                    euroId,
                    transactionAmount,
                    rate),
                foreignLine(
                    creditAccountId,
                    BigDecimal.ZERO,
                    baseAmount,
                    euroId,
                    transactionAmount,
                    rate)),
            null,
            0,
            NOW,
            "actor");
    repository.insertJournal(journal);
    final FinanceInfrastructureAdapters.PostingSnapshotAdapter adapter =
        new FinanceInfrastructureAdapters.PostingSnapshotAdapter(
            enterpriseReferences(),
            exchangeRateReferences(rate),
            dsl);

    final JournalPostingSnapshot snapshot =
        adapter.resolve(journal, Map.of("taxCategory", "STANDARD"), NOW);
    repository.savePostingSnapshot(snapshot);
    repository.saveJournal(journal.post());

    assertThat(repository.findPostingSnapshot(journal.id()))
        .get()
        .satisfies(
            persisted -> {
              assertThat(persisted.transactionCurrency()).isEqualTo("EUR");
              assertThat(persisted.baseCurrency()).isEqualTo("USD");
              assertThat(persisted.transactionAmount())
                  .isEqualByComparingTo(transactionAmount);
              assertThat(persisted.baseAmount()).isEqualByComparingTo(baseAmount);
              assertThat(persisted.exchangeRate()).isEqualByComparingTo(rate);
            });
  }

  @Test
  void rejectsUnauthorizedPostingBeforeSnapshotResolutionOrPersistence() {
    final JournalEntry draft = repository.insertJournal(journal("unauthorized-posting"));
    final AtomicBoolean snapshotResolved = new AtomicBoolean();
    final FinancePorts.AuthorizationPort denied =
        new FinancePorts.AuthorizationPort() {
          @Override
          public void authenticate(final String actor) {
            throw new AccessDeniedException("denied");
          }

          @Override
          public void require(
              final String actor, final String capability, final UUID scopedCompanyId) {
            throw new AssertionError("Company authorization must not follow failed authentication.");
          }

          @Override
          public void requireCostCenter(final String actor, final UUID costCenterId) {}

          @Override
          public void requireProfitCenter(final String actor, final UUID profitCenterId) {}

          @Override
          public void requireDimension(final String actor, final String dimensionCode) {}
        };
    final FinanceService deniedService =
        new FinanceService(
            repository,
            (scopedCompanyId, scopedBranchId) -> {},
            currencyId -> {},
            denied,
            (journal, taxContext, postedAt) -> {
              snapshotResolved.set(true);
              throw new AssertionError("Snapshot resolution must not occur.");
            },
            series -> series + "-" + UUID.randomUUID(),
            (actor, action, id) -> {},
            (type, aggregateId) -> {},
            (aggregateId, attachmentId) -> {},
            Clock.fixed(NOW, ZoneOffset.UTC));

    assertThatThrownBy(
            () ->
                deniedService.postJournal(
                    new FinanceCommands.PostJournal(
                        draft.id(),
                        AccountingPeriodContract.PostingPurpose.ORDINARY,
                        Map.of(),
                        "actor")))
        .isInstanceOf(AccessDeniedException.class);
    assertThat(snapshotResolved).isFalse();
    assertThat(repository.findJournal(draft.id()))
        .get()
        .extracting(JournalEntry::status)
        .isEqualTo(JournalEntry.JournalStatus.DRAFT);
    assertThat(repository.findPostingSnapshot(draft.id())).isEmpty();
  }

  @Test
  void atomicallyDeduplicatesConcurrentJournalCreation() throws Exception {
    final String key = "concurrent-" + UUID.randomUUID();
    final JournalEntry first = journal(key);
    final JournalEntry second = journal(key);
    final CountDownLatch start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      final var one =
          executor.submit(
              () -> {
                start.await();
                return new JooqFinanceRepository(newDsl(), new ObjectMapper()).insertJournal(first);
              });
      final var two =
          executor.submit(
              () -> {
                start.await();
                return new JooqFinanceRepository(newDsl(), new ObjectMapper()).insertJournal(second);
              });
      start.countDown();
      assertThat(List.of(one.get().id(), two.get().id()).stream().distinct()).hasSize(1);
    }
    assertThat(
            dsl.fetchCount(
                DSL.table("finance_journal_entry"),
                DSL.field("idempotency_key", String.class).eq(key)))
        .isEqualTo(1);
  }

  @Test
  void rejectsConcurrentDraftRevisionWithOptimisticLocking() {
    final JournalEntry draft = repository.insertJournal(journal("optimistic-lock"));
    final JournalEntry firstReader = repository.findJournal(draft.id()).orElseThrow();
    final JournalEntry secondReader = repository.findJournal(draft.id()).orElseThrow();

    final JournalEntry saved = repository.saveJournal(firstReader.revise(firstReader.lines()));

    assertThat(saved.lockVersion()).isEqualTo(1);
    assertThatThrownBy(
            () -> repository.saveJournal(secondReader.revise(secondReader.lines())))
        .isInstanceOf(com.newland.erp.finance.domain.FinanceException.class)
        .hasMessageContaining("concurrently");
  }

  @Test
  void assignsDurableIdempotentCompanyFiscalDocumentNumbers() {
    final FinanceFoundationContractAdapter contracts =
        new FinanceFoundationContractAdapter(repository, masterData(), dsl);
    final FinancialDocumentNumber.Request request =
        new FinancialDocumentNumber.Request(
            "JV",
            companyId,
            branchId,
            fiscalYearId,
            UUID.randomUUID(),
            "number-" + UUID.randomUUID());

    final FinancialDocumentNumber.Assignment first = contracts.assign(request);
    final FinancialDocumentNumber.Assignment retry = contracts.assign(request);

    assertThat(retry).isEqualTo(first);
    assertThat(first.number()).startsWith("JV-");
    assertThat(
            dsl.fetchCount(
                DSL.table("finance_document_number_assignment"),
                DSL.field("idempotency_key", String.class).eq(request.idempotencyKey())))
        .isEqualTo(1);
  }

  @Test
  void databaseRejectsUnbalancedPostedJournalAtCommit() {
    assertThatThrownBy(
            () ->
                dsl.transaction(
                    configuration -> {
                      final DSLContext transaction = DSL.using(configuration);
                      final JournalEntry journal = journal("unbalanced-db");
                      transaction
                          .insertInto(DSL.table("finance_journal_entry"))
                          .columns(
                              DSL.field("id"),
                              DSL.field("journal_number"),
                              DSL.field("idempotency_key"),
                              DSL.field("company_id"),
                              DSL.field("branch_id"),
                              DSL.field("fiscal_year_id"),
                              DSL.field("period_id"),
                              DSL.field("posting_date"),
                              DSL.field("status"),
                              DSL.field("lock_version"),
                              DSL.field("created_at"),
                              DSL.field("actor"))
                          .values(
                              journal.id(),
                              journal.number(),
                              journal.idempotencyKey(),
                              companyId,
                              branchId,
                              fiscalYearId,
                              periodId,
                              journal.postingDate(),
                              "DRAFT",
                              0,
                              OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC),
                              journal.actor())
                          .execute();
                      insertLine(transaction, journal.id(), debitAccountId, "100", "0");
                      insertLine(transaction, journal.id(), creditAccountId, "0", "90");
                      transaction
                          .update(DSL.table("finance_journal_entry"))
                          .set(DSL.field("status"), "POSTED")
                          .where(DSL.field("id", UUID.class).eq(journal.id()))
                          .execute();
                    }))
        .isInstanceOf(DataAccessException.class)
        .rootCause()
        .hasMessageContaining("balanced double-entry");
  }

  private JournalEntry journal(final String key) {
    final BigDecimal amount = new BigDecimal("100.000000");
    return new JournalEntry(
        UUID.randomUUID(),
        "JE-" + UUID.randomUUID(),
        key,
        companyId,
        branchId,
        fiscalYearId,
        periodId,
        LocalDate.of(2026, 7, 15),
        JournalEntry.JournalStatus.DRAFT,
        List.of(
            line(debitAccountId, amount, BigDecimal.ZERO),
            line(creditAccountId, BigDecimal.ZERO, amount)),
        null,
        0,
        NOW,
        UUID.randomUUID().toString());
  }

  private JournalEntry.JournalLine line(
      final UUID accountId, final BigDecimal debit, final BigDecimal credit) {
    return line(accountId, debit, credit, null, null);
  }

  private JournalEntry.JournalLine line(
      final UUID accountId,
      final BigDecimal debit,
      final BigDecimal credit,
      final UUID costCenterId,
      final UUID profitCenterId) {
    return new JournalEntry.JournalLine(
        UUID.randomUUID(),
        accountId,
        debit,
        credit,
        costCenterId,
        profitCenterId,
        null,
        null,
        debit.max(credit),
        BigDecimal.ONE);
  }

  private JournalEntry.JournalLine foreignLine(
      final UUID accountId,
      final BigDecimal debit,
      final BigDecimal credit,
      final UUID currencyId,
      final BigDecimal currencyAmount,
      final BigDecimal exchangeRate) {
    return new JournalEntry.JournalLine(
        UUID.randomUUID(),
        accountId,
        debit,
        credit,
        null,
        null,
        null,
        currencyId,
        currencyAmount,
        exchangeRate);
  }

  private FinanceCommands.CreateJournal createJournal(
      final String key, final JournalEntry.JournalLine... lines) {
    return new FinanceCommands.CreateJournal(
        key,
        companyId,
        branchId,
        fiscalYearId,
        periodId,
        LocalDate.of(2026, 7, 15),
        AccountingPeriodContract.PostingPurpose.ORDINARY,
        List.of(lines),
        List.of(),
        "actor");
  }

  private FinanceService service() {
    final FinancePorts.AuthorizationPort authorization =
        new FinancePorts.AuthorizationPort() {
          @Override
          public void authenticate(final String actor) {}

          @Override
          public void require(
              final String actor, final String capability, final UUID scopedCompanyId) {}

          @Override
          public void requireCostCenter(final String actor, final UUID costCenterId) {}

          @Override
          public void requireProfitCenter(final String actor, final UUID profitCenterId) {}

          @Override
          public void requireDimension(final String actor, final String dimensionCode) {}
        };
    return new FinanceService(
        repository,
        (scopedCompanyId, scopedBranchId) -> {},
        currencyId -> {},
        authorization,
        (journal, taxContext, postedAt) ->
            new JournalPostingSnapshot(
                journal.id(),
                "USD",
                "USD",
                null,
                "ENTERPRISE",
                "SPOT",
                journal.postingDate(),
                BigDecimal.ONE,
                journal.lines().stream()
                    .map(JournalEntry.JournalLine::debit)
                    .reduce(BigDecimal.ZERO, BigDecimal::add),
                journal.lines().stream()
                    .map(JournalEntry.JournalLine::debit)
                    .reduce(BigDecimal.ZERO, BigDecimal::add),
                taxContext,
                postedAt),
        series -> series + "-" + UUID.randomUUID(),
        (actor, action, id) -> {},
        (type, aggregateId) -> {},
        (aggregateId, attachmentId) -> {},
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private JournalPostingSnapshot snapshot(final UUID journalId) {
    return new JournalPostingSnapshot(
        journalId,
        "USD",
        "USD",
        null,
        "MASTER_DATA",
        "SPOT",
        LocalDate.of(2026, 7, 15),
        BigDecimal.ONE,
        new BigDecimal("100"),
        new BigDecimal("100"),
        Map.of("taxCategory", "NONE"),
        NOW);
  }

  private Account account(final UUID id, final String code) {
    return new Account(
        id, companyId, code, "Account " + code, Account.AccountType.ASSET, null, true, true);
  }

  private void seedCurrency(final UUID currencyId, final String code) {
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
            currencyId,
            "CURRENCY",
            code,
            code,
            true,
            JSONB.valueOf("{}"),
            0L,
            now,
            now)
        .execute();
  }

  private EnterpriseReferencePort enterpriseReferences() {
    return new EnterpriseReferencePort() {
      @Override
      public boolean isActiveCompany(final UUID scopedCompanyId) {
        return companyId.equals(scopedCompanyId);
      }

      @Override
      public boolean isActiveBranch(final UUID scopedCompanyId, final UUID scopedBranchId) {
        return companyId.equals(scopedCompanyId) && branchId.equals(scopedBranchId);
      }

      @Override
      public java.util.Optional<String> companyBaseCurrency(final UUID scopedCompanyId) {
        return companyId.equals(scopedCompanyId)
            ? java.util.Optional.of("USD")
            : java.util.Optional.empty();
      }
    };
  }

  private MasterDataReferencePort exchangeRateReferences(final BigDecimal rate) {
    return new MasterDataReferencePort() {
      @Override
      public boolean isActiveCurrency(final String currencyCode) {
        return "EUR".equals(currencyCode) || "USD".equals(currencyCode);
      }

      @Override
      public java.util.Optional<ExchangeRateSnapshot> resolveExchangeRate(
          final UUID scopedCompanyId,
          final String sourceCurrency,
          final String targetCurrency,
          final LocalDate effectiveDate) {
        if (!companyId.equals(scopedCompanyId)
            || !"EUR".equals(sourceCurrency)
            || !"USD".equals(targetCurrency)
            || !LocalDate.of(2026, 7, 15).equals(effectiveDate)) {
          return java.util.Optional.empty();
        }
        return java.util.Optional.of(
            new ExchangeRateSnapshot(
                UUID.randomUUID(),
                companyId,
                "EUR",
                "USD",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                rate));
      }
    };
  }

  private void seedCompany() {
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

  private static void insertLine(
      final DSLContext context,
      final UUID journalId,
      final UUID accountId,
      final String debit,
      final String credit) {
    context
        .insertInto(DSL.table("finance_journal_line"))
        .columns(
            DSL.field("id"),
            DSL.field("journal_id"),
            DSL.field("account_id"),
            DSL.field("debit"),
            DSL.field("credit"))
        .values(
            UUID.randomUUID(),
            journalId,
            accountId,
            new BigDecimal(debit),
            new BigDecimal(credit))
        .execute();
  }

  private DSLContext newDsl() {
    return DSL.using(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
  }

  private static MasterDataReferencePort masterData() {
    return new MasterDataReferencePort() {
      @Override
      public boolean isActiveCurrency(final String currencyCode) {
        return true;
      }

      @Override
      public java.util.Optional<ExchangeRateSnapshot> resolveExchangeRate(
          final UUID company,
          final String sourceCurrency,
          final String targetCurrency,
          final LocalDate effectiveDate) {
        return java.util.Optional.empty();
      }
    };
  }
}
