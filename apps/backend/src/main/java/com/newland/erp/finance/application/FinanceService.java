package com.newland.erp.finance.application;

import com.newland.erp.finance.domain.Account;
import com.newland.erp.finance.domain.AccountingPeriod;
import com.newland.erp.finance.domain.AccountingPeriodContract;
import com.newland.erp.finance.domain.ChartOfAccounts;
import com.newland.erp.finance.domain.FinanceException;
import com.newland.erp.finance.domain.FiscalYear;
import com.newland.erp.finance.domain.JournalEntry;
import com.newland.erp.finance.domain.JournalReversal;
import com.newland.erp.finance.domain.JournalPostingSnapshot;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public final class FinanceService {
  private final FinanceRepository repository;
  private final FinancePorts.EnterprisePort enterprise;
  private final FinancePorts.MasterDataPort masterData;
  private final FinancePorts.AuthorizationPort authorization;
  private final FinancePorts.PostingSnapshotPort snapshots;
  private final FinancePorts.NumberSeriesPort numbers;
  private final FinancePorts.AuditPort audit;
  private final FinancePorts.OutboxPort outbox;
  private final FinancePorts.AttachmentPort attachments;
  private final Clock clock;

  public FinanceService(
      final FinanceRepository financeRepository,
      final FinancePorts.EnterprisePort enterprisePort,
      final FinancePorts.MasterDataPort masterDataPort,
      final FinancePorts.AuthorizationPort authorizationPort,
      final FinancePorts.PostingSnapshotPort postingSnapshotPort,
      final FinancePorts.NumberSeriesPort numberSeriesPort,
      final FinancePorts.AuditPort auditPort,
      final FinancePorts.OutboxPort outboxPort,
      final FinancePorts.AttachmentPort attachmentPort,
      final Clock systemClock) {
    this.repository = financeRepository;
    this.enterprise = enterprisePort;
    this.masterData = masterDataPort;
    this.authorization = authorizationPort;
    this.snapshots = postingSnapshotPort;
    this.numbers = numberSeriesPort;
    this.audit = auditPort;
    this.outbox = outboxPort;
    this.attachments = attachmentPort;
    this.clock = systemClock;
  }

  @Transactional
  public Account createAccount(final FinanceCommands.CreateAccount command) {
    authorization.require(command.actor(), "finance.account.create", command.companyId());
    if (repository.accountCodeExists(command.companyId(), command.code().trim().toUpperCase())) {
      throw new FinanceException("Duplicate account code within company scope.");
    }
    final List<Account> accounts = repository.accounts(command.companyId());
    final UUID accountId = UUID.randomUUID();
    ChartOfAccounts.rejectCycle(accountId, command.parentId(), accounts);
    ChartOfAccounts.requireParentCompany(command.companyId(), command.parentId(), accounts);
    final Account account =
        new Account(
            accountId,
            command.companyId(),
            command.code(),
            command.name(),
            command.type(),
            command.parentId(),
            command.postable(),
            true);
    repository.saveAccount(account);
    audit.record(command.actor(), "FINANCE_ACCOUNT_CREATED", account.id());
    return account;
  }

  @Transactional
  public FiscalYear createFiscalYear(final FinanceCommands.CreateFiscalYear command) {
    authorization.require(command.actor(), "finance.fiscal-year.create", command.companyId());
    final FiscalYear year =
        new FiscalYear(
            UUID.randomUUID(),
            command.companyId(),
            command.code(),
            command.startsOn(),
            command.endsOn(),
            command.closed());
    repository.saveFiscalYear(year);
    return year;
  }

  @Transactional
  public AccountingPeriod createPeriod(final FinanceCommands.CreatePeriod command) {
    final FiscalYear year =
        repository
            .findFiscalYear(command.fiscalYearId())
            .orElseThrow(() -> new FinanceException("Fiscal year not found."));
    authorization.require(command.actor(), "finance.period.create", year.companyId());
    if (!year.contains(command.startsOn()) || !year.contains(command.endsOn())) {
      throw new FinanceException("Accounting period must be inside fiscal year boundaries.");
    }
    final AccountingPeriod period =
        new AccountingPeriod(
            UUID.randomUUID(),
            year.id(),
            command.code(),
            command.startsOn(),
            command.endsOn(),
            command.closed());
    repository.savePeriod(period);
    return period;
  }

  @Transactional
  public AccountingPeriod transitionPeriod(final FinanceCommands.TransitionPeriod command) {
    final AccountingPeriod current =
        repository
            .findPeriod(command.periodId())
            .orElseThrow(() -> new FinanceException("Accounting period not found."));
    final FiscalYear year =
        repository
            .findFiscalYear(current.fiscalYearId())
            .orElseThrow(() -> new FinanceException("Fiscal year not found."));
    authorization.require(command.actor(), "finance.period.manage", year.companyId());
    final AccountingPeriod changed = current.transitionTo(command.targetState());
    repository.updatePeriod(changed, current.state());
    audit.record(command.actor(), "FINANCE_ACCOUNTING_PERIOD_" + changed.state().name(), changed.id());
    outbox.publish("FinanceAccountingPeriod" + changed.state().name(), changed.id());
    return changed;
  }

  @Transactional
  public JournalEntry createJournal(final FinanceCommands.CreateJournal command) {
    authorization.require(command.actor(), "finance.journal.create", command.companyId());
    if (repository.idempotencyKeyExists(command.idempotencyKey())) {
      throw new FinanceException("Duplicate journal idempotency key.");
    }
    enterprise.requireCompanyBranch(command.companyId(), command.branchId());
    validatePeriod(
        command.companyId(),
        command.fiscalYearId(),
        command.periodId(),
        command.postingDate(),
        purpose(command.postingPurpose()));
    validateLines(command.actor(), command.companyId(), command.lines());
    final JournalEntry entry =
        new JournalEntry(
            UUID.randomUUID(),
            numbers.next("JE"),
            command.idempotencyKey(),
            command.companyId(),
            command.branchId(),
            command.fiscalYearId(),
            command.periodId(),
            command.postingDate(),
            JournalEntry.JournalStatus.DRAFT,
            command.lines(),
            null,
            0,
            Instant.now(clock),
            command.actor());
    repository.saveJournal(entry);
    command.attachmentIds().forEach(id -> attachments.attach(entry.id(), id));
    audit.record(command.actor(), "FINANCE_JOURNAL_DRAFT_CREATED", entry.id());
    return entry;
  }

  @Transactional
  public JournalEntry editJournal(final FinanceCommands.EditJournal command) {
    final JournalEntry original = journal(command.journalId());
    authorization.require(command.actor(), "finance.journal.edit", original.companyId());
    validateLines(command.actor(), original.companyId(), command.lines());
    final JournalEntry edited = original.revise(command.lines());
    return repository.saveJournal(edited);
  }

  @Transactional
  public JournalEntry postJournal(final FinanceCommands.PostJournal command) {
    authorization.authenticate(command.actor());
    final JournalEntry entry = journal(command.journalId());
    final AccountingPeriodContract.PostingPurpose postingPurpose =
        purpose(command.postingPurpose());
    authorizePosting(entry, postingPurpose, command.actor());
    final JournalPostingSnapshot snapshot =
        snapshots.resolve(entry, command.taxContext(), Instant.now(clock));
    return postJournalInternal(entry, postingPurpose, snapshot, command.actor());
  }

  @Transactional
  public JournalEntry postJournalWithSnapshot(
      final FinanceCommands.PostJournalWithSnapshot command) {
    authorization.authenticate(command.actor());
    final JournalEntry entry = journal(command.journalId());
    final AccountingPeriodContract.PostingPurpose postingPurpose =
        purpose(command.postingPurpose());
    authorizePosting(entry, postingPurpose, command.actor());
    return postJournalInternal(
        entry, postingPurpose, command.snapshot(), command.actor());
  }

  private JournalEntry postJournalInternal(
      final JournalEntry entry,
      final AccountingPeriodContract.PostingPurpose postingPurpose,
      final JournalPostingSnapshot snapshot,
      final String actor) {
    validatePeriod(
        entry.companyId(),
        entry.fiscalYearId(),
        entry.periodId(),
        entry.postingDate(),
        postingPurpose);
    validateLines(actor, entry.companyId(), entry.lines());
    if (snapshot == null || !snapshot.journalEntryId().equals(entry.id())) {
      throw new FinanceException("Posting snapshot journal scope is invalid.");
    }
    repository.savePostingSnapshot(snapshot);
    final JournalEntry posted = repository.saveJournal(entry.post());
    audit.record(actor, "FINANCE_JOURNAL_POSTED", posted.id());
    outbox.publish("FinanceJournalPosted", posted.id());
    return posted;
  }

  private void authorizePosting(
      final JournalEntry entry,
      final AccountingPeriodContract.PostingPurpose postingPurpose,
      final String actor) {
    authorization.require(actor, "finance.journal.post", entry.companyId());
    if (postingPurpose == AccountingPeriodContract.PostingPurpose.CLOSE_ADJUSTMENT) {
      authorization.require(actor, "finance.journal.close-adjustment.post", entry.companyId());
    }
  }

  @Transactional
  public JournalEntry createAndPostJournal(
      final String idempotencyKey,
      final UUID companyId,
      final UUID branchId,
      final java.time.LocalDate postingDate,
      final List<JournalEntry.JournalLine> lines,
      final String actor) {
    final var existing = repository.findJournalByIdempotencyKey(idempotencyKey);
    if (existing.isPresent()) {
      return resumeIdempotentPosting(
          existing.get(), companyId, branchId, postingDate, lines, actor);
    }
    final FinanceRepository.PostingPeriod period =
        repository
            .findOpenPostingPeriod(companyId, postingDate)
            .orElseThrow(() -> new FinanceException("No open accounting period exists."));
    final JournalEntry draft =
        createJournal(
            new FinanceCommands.CreateJournal(
                idempotencyKey,
                companyId,
                branchId,
                period.fiscalYearId(),
                period.periodId(),
                postingDate,
                AccountingPeriodContract.PostingPurpose.ORDINARY,
                lines,
                List.of(),
                actor));
    return postJournal(
        new FinanceCommands.PostJournal(
            draft.id(), AccountingPeriodContract.PostingPurpose.ORDINARY, java.util.Map.of(), actor));
  }

  @Transactional
  public JournalEntry createAndPostJournal(
      final String idempotencyKey,
      final UUID companyId,
      final UUID branchId,
      final java.time.LocalDate postingDate,
      final List<JournalEntry.JournalLine> lines,
      final JournalPostingSnapshotFactory snapshotFactory,
      final String actor) {
    final var existing = repository.findJournalByIdempotencyKey(idempotencyKey);
    if (existing.isPresent()) {
      final JournalEntry journal =
          resumeIdempotentPosting(
              existing.get(), companyId, branchId, postingDate, lines, actor);
      if (journal.status() == JournalEntry.JournalStatus.POSTED
          && repository.findPostingSnapshot(journal.id()).isEmpty()) {
        throw new FinanceException("Posted journal is missing its posting snapshot.");
      }
      return journal;
    }
    final FinanceRepository.PostingPeriod period =
        repository
            .findOpenPostingPeriod(companyId, postingDate)
            .orElseThrow(() -> new FinanceException("No open accounting period exists."));
    authorization.require(actor, "finance.journal.create", companyId);
    enterprise.requireCompanyBranch(companyId, branchId);
    validateLines(actor, companyId, lines);
    final JournalEntry draft =
        new JournalEntry(
            UUID.randomUUID(),
            numbers.next("JE"),
            idempotencyKey,
            companyId,
            branchId,
            period.fiscalYearId(),
            period.periodId(),
            postingDate,
            JournalEntry.JournalStatus.DRAFT,
            lines,
            null,
            0,
            Instant.now(clock),
            actor);
    final JournalEntry inserted = repository.insertJournal(draft);
    if (!inserted.id().equals(draft.id())) {
      return resumeIdempotentPosting(
          inserted, companyId, branchId, postingDate, lines, actor);
    }
    audit.record(actor, "FINANCE_JOURNAL_DRAFT_CREATED", draft.id());
    final JournalPostingSnapshot snapshot = snapshotFactory.create(draft.id());
    return postJournalWithSnapshot(
        new FinanceCommands.PostJournalWithSnapshot(
            draft.id(), AccountingPeriodContract.PostingPurpose.ORDINARY, snapshot, actor));
  }

  private JournalEntry resumeIdempotentPosting(
      final JournalEntry existing,
      final UUID companyId,
      final UUID branchId,
      final java.time.LocalDate postingDate,
      final List<JournalEntry.JournalLine> lines,
      final String actor) {
    if (!existing.companyId().equals(companyId)
        || !Objects.equals(existing.branchId(), branchId)
        || !existing.postingDate().equals(postingDate)
        || !existing.lines().equals(lines)) {
      throw new FinanceException("Journal idempotency key was reused with conflicting data.");
    }
    authorization.require(actor, "finance.journal.post", companyId);
    if (existing.status() == JournalEntry.JournalStatus.POSTED) {
      return existing;
    }
    if (existing.status() != JournalEntry.JournalStatus.DRAFT) {
      throw new FinanceException("Existing idempotent journal cannot be posted.");
    }
    return postJournal(
        new FinanceCommands.PostJournal(
            existing.id(),
            AccountingPeriodContract.PostingPurpose.ORDINARY,
            java.util.Map.of(),
            actor));
  }

  @Transactional
  public JournalEntry reverseJournal(final FinanceCommands.ReverseJournal command) {
    final JournalEntry original = journal(command.journalId());
    authorization.require(command.actor(), "finance.journal.reverse", original.companyId());
    if (original.status() != JournalEntry.JournalStatus.POSTED) {
      throw new FinanceException("Only posted journals can be reversed.");
    }
    if (repository.reversalExists(original.id())) {
      throw new FinanceException("A reversal already exists for this journal.");
    }
    if (repository.idempotencyKeyExists(command.idempotencyKey())) {
      throw new FinanceException("Duplicate journal idempotency key.");
    }
    final AccountingPeriodContract.PostingPurpose postingPurpose = purpose(command.postingPurpose());
    if (postingPurpose == AccountingPeriodContract.PostingPurpose.CLOSE_ADJUSTMENT) {
      authorization.require(
          command.actor(), "finance.journal.close-adjustment.post", original.companyId());
    }
    final FinanceRepository.PostingPeriod reversalPeriod =
        repository
            .findPostingPeriod(original.companyId(), command.postingDate(), postingPurpose)
            .orElseThrow(() -> new FinanceException("No eligible reversal accounting period exists."));
    final List<JournalEntry.JournalLine> lines =
        original.lines().stream()
            .map(
                line ->
                    new JournalEntry.JournalLine(
                        UUID.randomUUID(),
                        line.accountId(),
                        line.credit(),
                        line.debit(),
                        line.costCenterId(),
                        line.profitCenterId(),
                        line.dimensionCode(),
                        line.currencyId(),
                        line.currencyAmount() == null ? null : line.currencyAmount().negate(),
                        line.exchangeRateSnapshot()))
            .toList();
    final JournalEntry reversalDraft =
        new JournalEntry(
            UUID.randomUUID(),
            numbers.next("JR"),
            command.idempotencyKey(),
            original.companyId(),
            original.branchId(),
            reversalPeriod.fiscalYearId(),
            reversalPeriod.periodId(),
            command.postingDate(),
            JournalEntry.JournalStatus.DRAFT,
            lines,
            original.id(),
            0,
            Instant.now(clock),
            command.actor());
    final JournalEntry inserted = repository.insertJournal(reversalDraft);
    if (!inserted.id().equals(reversalDraft.id())) {
      if (inserted.reversalOfId() != null
          && inserted.reversalOfId().equals(original.id())
          && inserted.status() == JournalEntry.JournalStatus.POSTED) {
        return inserted;
      }
      throw new FinanceException("Reversal idempotency key was reused with conflicting data.");
    }
    final JournalPostingSnapshot reversalSnapshot =
        repository
        .findPostingSnapshot(original.id())
        .map(
            snapshot ->
                new JournalPostingSnapshot(
                    reversalDraft.id(),
                    snapshot.transactionCurrency(),
                    snapshot.baseCurrency(),
                    snapshot.exchangeRateId(),
                    snapshot.exchangeRateSource(),
                    snapshot.exchangeRateType(),
                    snapshot.exchangeRateDate(),
                    snapshot.exchangeRate(),
                    snapshot.transactionAmount(),
                    snapshot.baseAmount(),
                    snapshot.taxContext(),
                    Instant.now(clock)))
        .orElseThrow(() -> new FinanceException("Original journal posting snapshot is missing."));
    repository.savePostingSnapshot(reversalSnapshot);
    final JournalEntry reversal = repository.saveJournal(reversalDraft.post());
    repository.saveReversal(new JournalReversal(UUID.randomUUID(), original.id(), reversal.id()));
    audit.record(command.actor(), "FINANCE_JOURNAL_REVERSED", reversal.id());
    outbox.publish("FinanceJournalReversed", reversal.id());
    return reversal;
  }

  private JournalEntry journal(final UUID id) {
    return repository.findJournal(id).orElseThrow(() -> new FinanceException("Journal not found."));
  }

  private void validatePeriod(
      final UUID companyId,
      final UUID fiscalYearId,
      final UUID periodId,
      final java.time.LocalDate date,
      final AccountingPeriodContract.PostingPurpose postingPurpose) {
    final FiscalYear year =
        repository
            .findFiscalYear(fiscalYearId)
            .orElseThrow(() -> new FinanceException("Fiscal year not found."));
    final AccountingPeriod period =
        repository
            .findPeriod(periodId)
            .orElseThrow(() -> new FinanceException("Accounting period not found."));
    if (!year.companyId().equals(companyId)
        || !year.contains(date)
        || !period.fiscalYearId().equals(year.id())
        || !period.contains(date)) {
      throw new FinanceException(
          "Posting date is outside fiscal-year or accounting-period boundaries.");
    }
    if (year.closed()) {
      throw new FinanceException("Closed accounting periods cannot accept postings.");
    }
    period.requirePostingAllowed(date, postingPurpose);
  }

  private void validateLines(
      final String actor, final UUID companyId, final List<JournalEntry.JournalLine> lines) {
    JournalEntry.validateLines(lines);
    final List<Account> accounts = repository.accounts(companyId);
    for (final JournalEntry.JournalLine line : lines) {
      final Account account =
          accounts.stream()
              .filter(a -> a.id().equals(line.accountId()))
              .findFirst()
              .orElseThrow(
                  () -> new FinanceException("Account does not belong to journal company scope."));
      account.requirePostable();
      if (line.currencyId() != null) {
        masterData.requireCurrency(line.currencyId());
      }
      if (line.costCenterId() != null) {
        final var costCenter =
            repository
                .findCostCenter(companyId, line.costCenterId())
                .orElseThrow(
                    () ->
                        new FinanceException(
                            "Cost center is inactive or outside journal company scope."));
        if (!costCenter.active()) {
          throw new FinanceException("Cost center is inactive or outside journal company scope.");
        }
        authorization.requireCostCenter(actor, line.costCenterId());
      }
      if (line.profitCenterId() != null) {
        final var profitCenter =
            repository
                .findProfitCenter(companyId, line.profitCenterId())
                .orElseThrow(
                    () ->
                        new FinanceException(
                            "Profit center is inactive or outside journal company scope."));
        if (!profitCenter.active()) {
          throw new FinanceException("Profit center is inactive or outside journal company scope.");
        }
        authorization.requireProfitCenter(actor, line.profitCenterId());
      }
      if (line.dimensionCode() != null) {
        authorization.requireDimension(actor, line.dimensionCode());
      }
    }
  }

  private static AccountingPeriodContract.PostingPurpose purpose(
      final AccountingPeriodContract.PostingPurpose value) {
    return value == null ? AccountingPeriodContract.PostingPurpose.ORDINARY : value;
  }

  @FunctionalInterface
  public interface JournalPostingSnapshotFactory {
    JournalPostingSnapshot create(UUID journalEntryId);
  }
}
