package com.newland.erp.finance.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newland.erp.finance.domain.Account;
import com.newland.erp.finance.domain.AccountingPeriod;
import com.newland.erp.finance.domain.FiscalYear;
import com.newland.erp.finance.domain.FinancialDocumentNumber;
import com.newland.erp.finance.domain.JournalEntry;
import com.newland.erp.finance.domain.JournalPostingSnapshot;
import com.newland.erp.masterdata.application.integration.MasterDataReferencePort;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
    repository.saveJournal(draft.post());
    final JournalPostingSnapshot snapshot =
        snapshot(draft.id());
    repository.savePostingSnapshot(snapshot);

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
    return new JournalEntry.JournalLine(
        UUID.randomUUID(),
        accountId,
        debit,
        credit,
        null,
        null,
        null,
        null,
        debit.max(credit),
        BigDecimal.ONE);
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
        Map.of("taxCategory", "NONE"));
  }

  private Account account(final UUID id, final String code) {
    return new Account(
        id, companyId, code, "Account " + code, Account.AccountType.ASSET, null, true, true);
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
