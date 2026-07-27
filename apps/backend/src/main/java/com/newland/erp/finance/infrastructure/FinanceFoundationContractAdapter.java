package com.newland.erp.finance.infrastructure;

import com.newland.erp.finance.application.FinanceRepository;
import com.newland.erp.finance.application.integration.AccountingPeriodContractPort;
import com.newland.erp.finance.application.integration.CurrencyExchangeContractPort;
import com.newland.erp.finance.application.integration.FinancialDocumentNumberPort;
import com.newland.erp.finance.application.integration.JournalEntryContractPort;
import com.newland.erp.finance.domain.AccountingPeriodContract;
import com.newland.erp.finance.domain.CurrencyExchangeContract;
import com.newland.erp.finance.domain.FinanceException;
import com.newland.erp.finance.domain.FinancialDocumentNumber;
import com.newland.erp.finance.domain.JournalEntry;
import com.newland.erp.finance.domain.JournalEntryContract;
import com.newland.erp.masterdata.application.integration.MasterDataReferencePort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Durable adapters implementing the published P3.12 Finance foundation contracts. */
@Component
public final class FinanceFoundationContractAdapter
    implements AccountingPeriodContractPort,
        JournalEntryContractPort,
        FinancialDocumentNumberPort,
        CurrencyExchangeContractPort {
  private final FinanceRepository finance;
  private final MasterDataReferencePort masterData;
  private final DSLContext dsl;

  public FinanceFoundationContractAdapter(
      final FinanceRepository financeRepository,
      final MasterDataReferencePort masterDataReferencePort,
      final DSLContext dslContext) {
    finance = financeRepository;
    masterData = masterDataReferencePort;
    dsl = dslContext;
  }

  @Override
  @Transactional(readOnly = true)
  public AccountingPeriodContract requirePostingPeriod(
      final UUID companyId,
      final java.time.LocalDate postingDate,
      final AccountingPeriodContract.PostingPurpose purpose) {
    final FinanceRepository.PostingPeriod resolved =
        finance
            .findPostingPeriod(companyId, postingDate, purpose)
            .orElseThrow(() -> new FinanceException("No eligible accounting period exists."));
    final var period =
        finance
            .findPeriod(resolved.periodId())
            .orElseThrow(() -> new FinanceException("Accounting period not found."));
    period.requirePostingAllowed(postingDate, purpose);
    return new AccountingPeriodContract(
        companyId,
        resolved.fiscalYearId(),
        period.id(),
        period.code(),
        period.startsOn(),
        period.endsOn(),
        AccountingPeriodContract.State.valueOf(period.state().name()));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<JournalEntryContract> findBySource(
      final UUID companyId, final String sourceDocumentType, final UUID sourceDocumentId) {
    return dsl.select(DSL.field("journal_entry_id", UUID.class))
        .from(DSL.table("finance_posting_request").as("request"))
        .join(DSL.table("finance_accounting_event").as("event"))
        .on(
            DSL.field("event.accounting_event_id", UUID.class)
                .eq(DSL.field("request.accounting_event_id", UUID.class)))
        .where(DSL.field("event.company_id", UUID.class).eq(companyId))
        .and(DSL.field("event.source_document_type", String.class).eq(sourceDocumentType))
        .and(DSL.field("event.source_document_id", UUID.class).eq(sourceDocumentId))
        .and(DSL.field("request.journal_entry_id", UUID.class).isNotNull())
        .fetchOptional(0, UUID.class)
        .flatMap(id -> finance.findJournal(id).map(this::contract));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<JournalEntryContract> findPostedJournal(
      final UUID companyId, final UUID journalEntryId) {
    return finance
        .findJournal(journalEntryId)
        .filter(journal -> journal.companyId().equals(companyId))
        .filter(journal -> journal.status() == JournalEntry.JournalStatus.POSTED)
        .map(this::contract);
  }

  @Override
  @Transactional
  public FinancialDocumentNumber.Assignment assign(
      final FinancialDocumentNumber.Request request) {
    final var year =
        finance
            .findFiscalYear(request.fiscalYearId())
            .orElseThrow(() -> new FinanceException("Fiscal year not found."));
    if (!year.companyId().equals(request.companyId())) {
      throw new FinanceException("Financial number fiscal year is outside company scope.");
    }
    final Optional<FinancialDocumentNumber.Assignment> existing =
        findNumberAssignment(request.idempotencyKey());
    if (existing.isPresent()) {
      requireSameAssignmentScope(request, existing.get());
      return existing.get();
    }
    final UUID branchScope =
        request.branchId() == null ? new UUID(0L, 0L) : request.branchId();
    final Long sequence =
        dsl.insertInto(DSL.table("finance_document_number_counter"))
            .columns(
                DSL.field("document_type"),
                DSL.field("company_id"),
                DSL.field("scope_branch_id"),
                DSL.field("fiscal_year_id"),
                DSL.field("next_value"))
            .values(
                request.documentType(),
                request.companyId(),
                branchScope,
                request.fiscalYearId(),
                2L)
            .onConflict(
                DSL.field("document_type"),
                DSL.field("company_id"),
                DSL.field("scope_branch_id"),
                DSL.field("fiscal_year_id"))
            .doUpdate()
            .set(
                DSL.field("next_value", Long.class),
                DSL.field("finance_document_number_counter.next_value", Long.class).plus(1L))
            .returning(DSL.field("next_value", Long.class))
            .fetchOne(DSL.field("next_value", Long.class));
    if (sequence == null) {
      throw new FinanceException("Financial document number could not be allocated.");
    }
    final long assignedValue = sequence - 1L;
    final String number =
        request.documentType()
            + "-"
            + request.fiscalYearId().toString().substring(0, 8).toUpperCase()
            + "-"
            + String.format(java.util.Locale.ROOT, "%010d", assignedValue);
    final Instant assignedAt = Instant.now();
    final int inserted =
        dsl.insertInto(DSL.table("finance_document_number_assignment"))
            .columns(
                DSL.field("document_id"),
                DSL.field("idempotency_key"),
                DSL.field("document_type"),
                DSL.field("company_id"),
                DSL.field("branch_id"),
                DSL.field("fiscal_year_id"),
                DSL.field("assigned_number"),
                DSL.field("assigned_at"))
            .values(
                request.documentId(),
                request.idempotencyKey(),
                request.documentType(),
                request.companyId(),
                request.branchId(),
                request.fiscalYearId(),
                number,
                java.time.OffsetDateTime.ofInstant(assignedAt, java.time.ZoneOffset.UTC))
            .onConflict(DSL.field("idempotency_key"))
            .doNothing()
            .execute();
    if (inserted == 0) {
      final FinancialDocumentNumber.Assignment concurrent =
          findNumberAssignment(request.idempotencyKey())
              .orElseThrow(
                  () -> new FinanceException("Concurrent financial number allocation failed."));
      requireSameAssignmentScope(request, concurrent);
      return concurrent;
    }
    return findNumberAssignment(request.idempotencyKey())
        .orElseThrow(() -> new FinanceException("Financial number assignment was not persisted."));
  }

  @Override
  @Transactional(readOnly = true)
  public CurrencyExchangeContract.Currency requireActiveCurrency(final String currencyCode) {
    if (!masterData.isActiveCurrency(currencyCode)) {
      throw new FinanceException("Currency is missing or inactive.");
    }
    final Integer fractionDigits =
        dsl.select(
                DSL.field("(attributes ->> 'fractionDigits')::integer", Integer.class))
            .from(DSL.table("master_data_record"))
            .where(DSL.field("aggregate_type", String.class).eq("CURRENCY"))
            .and(DSL.field("code", String.class).eq(currencyCode.toUpperCase()))
            .and(DSL.field("active", Boolean.class).eq(true))
            .fetchOne(0, Integer.class);
    return new CurrencyExchangeContract.Currency(
        currencyCode, fractionDigits == null ? 2 : fractionDigits, true);
  }

  @Override
  @Transactional(readOnly = true)
  public CurrencyExchangeContract.RateSnapshot requireRate(
      final CurrencyExchangeContract.RateQuery query) {
    final var rate =
        masterData
            .resolveExchangeRate(
                query.companyId(),
                query.sourceCurrency(),
                query.targetCurrency(),
                query.effectiveDate())
            .orElseThrow(() -> new FinanceException("Applicable exchange rate is unavailable."));
    final CurrencyExchangeContract.RateSnapshot snapshot =
        new CurrencyExchangeContract.RateSnapshot(
            rate.rateId(),
            rate.companyId(),
            rate.sourceCurrency(),
            rate.targetCurrency(),
            query.rateType(),
            "MASTER_DATA",
            rate.validFrom(),
            rate.validTo(),
            rate.rate());
    snapshot.requireMatches(query);
    return snapshot;
  }

  private JournalEntryContract contract(final JournalEntry journal) {
    final var snapshot = finance.findPostingSnapshot(journal.id());
    final String currency =
        snapshot
            .map(value -> value.transactionCurrency())
            .orElseGet(() -> companyBaseCurrency(journal.companyId()));
    final BigDecimal rate =
        snapshot.map(value -> value.exchangeRate()).orElse(BigDecimal.ONE);
    final List<JournalEntryContract.Line> lines =
        journal.lines().stream()
            .map(line -> contract(line, currency, rate))
            .toList();
    return new JournalEntryContract(
        journal.id(),
        journal.number(),
        journal.idempotencyKey(),
        journal.companyId(),
        journal.branchId(),
        journal.fiscalYearId(),
        journal.periodId(),
        journal.postingDate(),
        JournalEntryContract.Status.valueOf(journal.status().name()),
        "",
        null,
        journal.reversalOfId(),
        lines,
        journal.lockVersion());
  }

  private Optional<FinancialDocumentNumber.Assignment> findNumberAssignment(
      final String idempotencyKey) {
    return dsl.selectFrom(DSL.table("finance_document_number_assignment"))
        .where(DSL.field("idempotency_key", String.class).eq(idempotencyKey))
        .fetchOptional(
            row ->
                new FinancialDocumentNumber.Assignment(
                    row.get("assigned_number", String.class),
                    row.get("document_type", String.class),
                    row.get("company_id", UUID.class),
                    row.get("fiscal_year_id", UUID.class),
                    row.get("document_id", UUID.class),
                    row.get("assigned_at", java.time.OffsetDateTime.class).toInstant()));
  }

  private static void requireSameAssignmentScope(
      final FinancialDocumentNumber.Request request,
      final FinancialDocumentNumber.Assignment assignment) {
    if (!request.documentType().equals(assignment.documentType())
        || !request.companyId().equals(assignment.companyId())
        || !request.fiscalYearId().equals(assignment.fiscalYearId())
        || !request.documentId().equals(assignment.documentId())) {
      throw new FinanceException(
          "Financial number idempotency key was reused with conflicting scope.");
    }
  }

  private String companyBaseCurrency(final UUID companyId) {
    return dsl.select(DSL.field("base_currency", String.class))
        .from(DSL.table("company"))
        .where(DSL.field("id", UUID.class).eq(companyId))
        .fetchOptional(0, String.class)
        .orElseThrow(() -> new FinanceException("Journal company base currency is unavailable."));
  }

  private static JournalEntryContract.Line contract(
      final JournalEntry.JournalLine line, final String currency, final BigDecimal rate) {
    final BigDecimal amount =
        line.currencyAmount() == null
            ? line.debit().max(line.credit())
            : line.currencyAmount().abs();
    return new JournalEntryContract.Line(
        line.id(),
        line.accountId(),
        line.debit(),
        line.credit(),
        line.costCenterId(),
        line.profitCenterId(),
        line.dimensionCode(),
        currency,
        amount,
        line.exchangeRateSnapshot() == null ? rate : line.exchangeRateSnapshot());
  }
}
