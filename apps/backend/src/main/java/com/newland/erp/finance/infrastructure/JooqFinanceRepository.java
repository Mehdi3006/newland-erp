package com.newland.erp.finance.infrastructure;

import com.newland.erp.finance.application.FinanceRepository;
import com.newland.erp.finance.domain.Account;
import com.newland.erp.finance.domain.AccountingPeriod;
import com.newland.erp.finance.domain.CostCenter;
import com.newland.erp.finance.domain.FinanceException;
import com.newland.erp.finance.domain.FiscalYear;
import com.newland.erp.finance.domain.JournalEntry;
import com.newland.erp.finance.domain.JournalReversal;
import com.newland.erp.finance.domain.ProfitCenter;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

@Repository
public final class JooqFinanceRepository implements FinanceRepository {
  private final DSLContext dsl;

  public JooqFinanceRepository(final DSLContext dslContext) {
    this.dsl = dslContext;
  }

  @Override
  public boolean idempotencyKeyExists(final String key) {
    return dsl.fetchExists(
        DSL.table("finance_journal_entry"), DSL.field("idempotency_key", String.class).eq(key));
  }

  @Override
  public boolean accountCodeExists(final UUID companyId, final String code) {
    return dsl.fetchExists(
        DSL.table("finance_account"),
        DSL.field("company_id", UUID.class)
            .eq(companyId)
            .and(DSL.field("account_code", String.class).eq(code)));
  }

  @Override
  public List<Account> accounts(final UUID companyId) {
    return dsl.selectFrom(DSL.table("finance_account"))
        .where(DSL.field("company_id", UUID.class).eq(companyId))
        .fetch(this::account);
  }

  @Override
  public Optional<Account> findAccount(final UUID companyId, final UUID accountId) {
    return dsl.selectFrom(DSL.table("finance_account"))
        .where(DSL.field("company_id", UUID.class).eq(companyId))
        .and(DSL.field("id", UUID.class).eq(accountId))
        .fetchOptional(this::account);
  }

  @Override
  public Optional<CostCenter> findCostCenter(final UUID companyId, final UUID costCenterId) {
    return dsl.selectFrom(DSL.table("finance_cost_center"))
        .where(DSL.field("company_id", UUID.class).eq(companyId))
        .and(DSL.field("id", UUID.class).eq(costCenterId))
        .fetchOptional(
            row ->
                new CostCenter(
                    row.get("id", UUID.class),
                    row.get("company_id", UUID.class),
                    row.get("code", String.class),
                    row.get("active", Boolean.class)));
  }

  @Override
  public Optional<ProfitCenter> findProfitCenter(
      final UUID companyId, final UUID profitCenterId) {
    return dsl.selectFrom(DSL.table("finance_profit_center"))
        .where(DSL.field("company_id", UUID.class).eq(companyId))
        .and(DSL.field("id", UUID.class).eq(profitCenterId))
        .fetchOptional(
            row ->
                new ProfitCenter(
                    row.get("id", UUID.class),
                    row.get("company_id", UUID.class),
                    row.get("code", String.class),
                    row.get("active", Boolean.class)));
  }

  @Override
  public boolean financialDimensionIsActive(
      final UUID companyId, final String dimensionCode) {
    return dsl.fetchExists(
        DSL.table("finance_financial_dimension"),
        DSL.field("company_id", UUID.class)
            .eq(companyId)
            .and(DSL.field("code", String.class).eq(dimensionCode))
            .and(DSL.field("active", Boolean.class).eq(true)));
  }

  @Override
  public Account saveAccount(final Account a) {
    if (dsl.fetchExists(DSL.table("finance_account"), DSL.field("id", UUID.class).eq(a.id()))) {
      dsl.update(DSL.table("finance_account"))
          .set(DSL.field("active", Boolean.class), a.active())
          .execute();
    } else {
      dsl.insertInto(DSL.table("finance_account"))
          .columns(
              DSL.field("id", UUID.class),
              DSL.field("company_id", UUID.class),
              DSL.field("account_code", String.class),
              DSL.field("name", String.class),
              DSL.field("account_type", String.class),
              DSL.field("parent_account_id", UUID.class),
              DSL.field("postable", Boolean.class),
              DSL.field("active", Boolean.class))
          .values(
              a.id(),
              a.companyId(),
              a.code(),
              a.name(),
              a.type().name(),
              a.parentId(),
              a.postable(),
              a.active())
          .execute();
    }
    return a;
  }

  @Override
  public FiscalYear saveFiscalYear(final FiscalYear y) {
    dsl.insertInto(DSL.table("finance_fiscal_year"))
        .columns(
            DSL.field("id", UUID.class),
            DSL.field("company_id", UUID.class),
            DSL.field("fiscal_year_code", String.class),
            DSL.field("starts_on", LocalDate.class),
            DSL.field("ends_on", LocalDate.class),
            DSL.field("closed", Boolean.class))
        .values(y.id(), y.companyId(), y.code(), y.startsOn(), y.endsOn(), y.closed())
        .execute();
    return y;
  }

  @Override
  public AccountingPeriod savePeriod(final AccountingPeriod p) {
    dsl.insertInto(DSL.table("finance_accounting_period"))
        .columns(
            DSL.field("id", UUID.class),
            DSL.field("fiscal_year_id", UUID.class),
            DSL.field("period_code", String.class),
            DSL.field("starts_on", LocalDate.class),
            DSL.field("ends_on", LocalDate.class),
            DSL.field("closed", Boolean.class))
        .values(p.id(), p.fiscalYearId(), p.code(), p.startsOn(), p.endsOn(), p.closed())
        .execute();
    return p;
  }

  @Override
  public Optional<FiscalYear> findFiscalYear(final UUID id) {
    return dsl.selectFrom(DSL.table("finance_fiscal_year"))
        .where(DSL.field("id", UUID.class).eq(id))
        .fetchOptional(
            r ->
                new FiscalYear(
                    r.get("id", UUID.class),
                    r.get("company_id", UUID.class),
                    r.get("fiscal_year_code", String.class),
                    r.get("starts_on", LocalDate.class),
                    r.get("ends_on", LocalDate.class),
                    r.get("closed", Boolean.class)));
  }

  @Override
  public Optional<AccountingPeriod> findPeriod(final UUID id) {
    return dsl.selectFrom(DSL.table("finance_accounting_period"))
        .where(DSL.field("id", UUID.class).eq(id))
        .fetchOptional(
            r ->
                new AccountingPeriod(
                    r.get("id", UUID.class),
                    r.get("fiscal_year_id", UUID.class),
                    r.get("period_code", String.class),
                    r.get("starts_on", LocalDate.class),
                    r.get("ends_on", LocalDate.class),
                    r.get("closed", Boolean.class)));
  }

  @Override
  public Optional<FinanceRepository.PostingPeriod> findOpenPostingPeriod(
      final UUID companyId, final LocalDate postingDate) {
    final List<FinanceRepository.PostingPeriod> matches = dsl.select(
            DSL.field("fy.id", UUID.class), DSL.field("p.id", UUID.class))
        .from(DSL.table("finance_fiscal_year").as("fy"))
        .join(DSL.table("finance_accounting_period").as("p"))
        .on(DSL.field("p.fiscal_year_id", UUID.class).eq(DSL.field("fy.id", UUID.class)))
        .where(DSL.field("fy.company_id", UUID.class).eq(companyId))
        .and(DSL.field("fy.closed", Boolean.class).eq(false))
        .and(DSL.field("p.closed", Boolean.class).eq(false))
        .and(
            DSL.val(postingDate)
                .between(
                    DSL.field("fy.starts_on", LocalDate.class),
                    DSL.field("fy.ends_on", LocalDate.class)))
        .and(
            DSL.val(postingDate)
                .between(
                    DSL.field("p.starts_on", LocalDate.class),
                    DSL.field("p.ends_on", LocalDate.class)))
        .orderBy(DSL.field("fy.id"), DSL.field("p.id"))
        .limit(2)
        .fetch(r -> new FinanceRepository.PostingPeriod(r.value1(), r.value2()));
    if (matches.size() > 1) {
      throw new FinanceException(
          "Posting date resolves to multiple open accounting periods.");
    }
    return matches.stream().findFirst();
  }

  @Override
  public JournalEntry saveJournal(final JournalEntry j) {
    final var table = DSL.table("finance_journal_entry");
    final boolean exists = dsl.fetchExists(table, DSL.field("id", UUID.class).eq(j.id()));
    if (exists) {
      final int updated =
          dsl.update(table)
              .set(DSL.field("status", String.class), j.status().name())
              .set(DSL.field("lock_version", Integer.class), j.lockVersion() + 1)
              .where(
                  DSL.field("id", UUID.class)
                      .eq(j.id())
                      .and(DSL.field("lock_version", Integer.class).eq(j.lockVersion())))
              .execute();
      if (updated != 1) {
        throw new FinanceException("Journal was modified concurrently.");
      }
      if (j.status() == JournalEntry.JournalStatus.DRAFT) {
        dsl.deleteFrom(DSL.table("finance_journal_line"))
            .where(DSL.field("journal_id", UUID.class).eq(j.id()))
            .execute();
      }
    } else {
      dsl.insertInto(table)
          .columns(
              DSL.field("id", UUID.class),
              DSL.field("journal_number", String.class),
              DSL.field("idempotency_key", String.class),
              DSL.field("company_id", UUID.class),
              DSL.field("branch_id", UUID.class),
              DSL.field("fiscal_year_id", UUID.class),
              DSL.field("period_id", UUID.class),
              DSL.field("posting_date", LocalDate.class),
              DSL.field("status", String.class),
              DSL.field("reversal_of_id", UUID.class),
              DSL.field("lock_version", Integer.class),
              DSL.field("created_at", OffsetDateTime.class),
              DSL.field("actor", String.class))
          .values(
              j.id(),
              j.number(),
              j.idempotencyKey(),
              j.companyId(),
              j.branchId(),
              j.fiscalYearId(),
              j.periodId(),
              j.postingDate(),
              j.status().name(),
              j.reversalOfId(),
              j.lockVersion(),
              OffsetDateTime.ofInstant(j.createdAt(), ZoneOffset.UTC),
              j.actor())
          .execute();
    }
    if (!exists || j.status() == JournalEntry.JournalStatus.DRAFT) {
      j.lines()
          .forEach(
              l ->
                  dsl.insertInto(DSL.table("finance_journal_line"))
                    .columns(
                        DSL.field("id", UUID.class),
                        DSL.field("journal_id", UUID.class),
                        DSL.field("account_id", UUID.class),
                        DSL.field("debit", java.math.BigDecimal.class),
                        DSL.field("credit", java.math.BigDecimal.class),
                        DSL.field("cost_center_id", UUID.class),
                        DSL.field("profit_center_id", UUID.class),
                        DSL.field("dimension_code", String.class),
                        DSL.field("currency_id", UUID.class),
                        DSL.field("currency_amount", java.math.BigDecimal.class),
                        DSL.field("exchange_rate_snapshot", java.math.BigDecimal.class))
                    .values(
                        l.id(),
                        j.id(),
                        l.accountId(),
                        l.debit(),
                        l.credit(),
                        l.costCenterId(),
                        l.profitCenterId(),
                        l.dimensionCode(),
                        l.currencyId(),
                        l.currencyAmount(),
                        l.exchangeRateSnapshot())
                      .execute());
    }
    return j;
  }

  @Override
  public Optional<JournalEntry> findJournal(final UUID id) {
    return dsl.selectFrom(DSL.table("finance_journal_entry"))
        .where(DSL.field("id", UUID.class).eq(id))
        .fetchOptional(this::journal);
  }

  @Override
  public Optional<JournalEntry> findJournalByIdempotencyKey(final String idempotencyKey) {
    return dsl.selectFrom(DSL.table("finance_journal_entry"))
        .where(DSL.field("idempotency_key", String.class).eq(idempotencyKey))
        .fetchOptional(this::journal);
  }

  @Override
  public boolean reversalExists(final UUID id) {
    return dsl.fetchExists(
        DSL.table("finance_journal_reversal"), DSL.field("original_journal_id", UUID.class).eq(id));
  }

  @Override
  public JournalReversal saveReversal(final JournalReversal r) {
    dsl.insertInto(DSL.table("finance_journal_reversal"))
        .columns(
            DSL.field("id", UUID.class),
            DSL.field("original_journal_id", UUID.class),
            DSL.field("reversal_journal_id", UUID.class))
        .values(r.id(), r.originalJournalId(), r.reversalJournalId())
        .execute();
    return r;
  }

  private Account account(final Record r) {
    return new Account(
        r.get("id", UUID.class),
        r.get("company_id", UUID.class),
        r.get("account_code", String.class),
        r.get("name", String.class),
        Account.AccountType.valueOf(r.get("account_type", String.class)),
        r.get("parent_account_id", UUID.class),
        r.get("postable", Boolean.class),
        r.get("active", Boolean.class));
  }

  private JournalEntry journal(final Record r) {
    UUID id = r.get("id", UUID.class);
    List<JournalEntry.JournalLine> lines =
        dsl.selectFrom(DSL.table("finance_journal_line"))
            .where(DSL.field("journal_id", UUID.class).eq(id))
            .fetch(
                l ->
                    new JournalEntry.JournalLine(
                        l.get("id", UUID.class),
                        l.get("account_id", UUID.class),
                        l.get("debit", java.math.BigDecimal.class),
                        l.get("credit", java.math.BigDecimal.class),
                        l.get("cost_center_id", UUID.class),
                        l.get("profit_center_id", UUID.class),
                        l.get("dimension_code", String.class),
                        l.get("currency_id", UUID.class),
                        l.get("currency_amount", java.math.BigDecimal.class),
                        l.get("exchange_rate_snapshot", java.math.BigDecimal.class)));
    OffsetDateTime created = r.get("created_at", OffsetDateTime.class);
    return new JournalEntry(
        id,
        r.get("journal_number", String.class),
        r.get("idempotency_key", String.class),
        r.get("company_id", UUID.class),
        r.get("branch_id", UUID.class),
        r.get("fiscal_year_id", UUID.class),
        r.get("period_id", UUID.class),
        r.get("posting_date", LocalDate.class),
        JournalEntry.JournalStatus.valueOf(r.get("status", String.class)),
        lines,
        r.get("reversal_of_id", UUID.class),
        r.get("lock_version", Integer.class),
        created.toInstant(),
        r.get("actor", String.class));
  }
}
