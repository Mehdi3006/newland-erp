package com.newland.erp.finance.application;

import com.newland.erp.finance.domain.Account;
import com.newland.erp.finance.domain.AccountingPeriod;
import com.newland.erp.finance.domain.ChartOfAccounts;
import com.newland.erp.finance.domain.FinanceException;
import com.newland.erp.finance.domain.FiscalYear;
import com.newland.erp.finance.domain.JournalEntry;
import com.newland.erp.finance.domain.JournalReversal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public final class FinanceService {
  private final FinanceRepository repository;
  private final FinancePorts.EnterprisePort enterprise;
  private final FinancePorts.MasterDataPort masterData;
  private final FinancePorts.AuthorizationPort authorization;
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
      final FinancePorts.NumberSeriesPort numberSeriesPort,
      final FinancePorts.AuditPort auditPort,
      final FinancePorts.OutboxPort outboxPort,
      final FinancePorts.AttachmentPort attachmentPort,
      final Clock systemClock) {
    this.repository = financeRepository;
    this.enterprise = enterprisePort;
    this.masterData = masterDataPort;
    this.authorization = authorizationPort;
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
  public JournalEntry createJournal(final FinanceCommands.CreateJournal command) {
    authorization.require(command.actor(), "finance.journal.create", command.companyId());
    if (repository.idempotencyKeyExists(command.idempotencyKey())) {
      throw new FinanceException("Duplicate journal idempotency key.");
    }
    enterprise.requireCompanyBranch(command.companyId(), command.branchId());
    validatePeriod(
        command.companyId(), command.fiscalYearId(), command.periodId(), command.postingDate());
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
    repository.saveJournal(edited);
    return edited;
  }

  @Transactional
  public JournalEntry postJournal(final FinanceCommands.PostJournal command) {
    final JournalEntry entry = journal(command.journalId());
    authorization.require(command.actor(), "finance.journal.post", entry.companyId());
    validatePeriod(entry.companyId(), entry.fiscalYearId(), entry.periodId(), entry.postingDate());
    validateLines(command.actor(), entry.companyId(), entry.lines());
    final JournalEntry posted = entry.post();
    repository.saveJournal(posted);
    audit.record(command.actor(), "FINANCE_JOURNAL_POSTED", posted.id());
    outbox.publish("FinanceJournalPosted", posted.id());
    return posted;
  }

  @Transactional
  public JournalEntry reverseJournal(final FinanceCommands.ReverseJournal command) {
    final JournalEntry original = journal(command.journalId());
    authorization.require(command.actor(), "finance.journal.reverse", original.companyId());
    validatePeriod(
        original.companyId(), original.fiscalYearId(), original.periodId(), original.postingDate());
    if (original.status() != JournalEntry.JournalStatus.POSTED) {
      throw new FinanceException("Only posted journals can be reversed.");
    }
    if (repository.reversalExists(original.id())) {
      throw new FinanceException("A reversal already exists for this journal.");
    }
    if (repository.idempotencyKeyExists(command.idempotencyKey())) {
      throw new FinanceException("Duplicate journal idempotency key.");
    }
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
    final JournalEntry reversal =
        new JournalEntry(
                UUID.randomUUID(),
                numbers.next("JR"),
                command.idempotencyKey(),
                original.companyId(),
                original.branchId(),
                original.fiscalYearId(),
                original.periodId(),
                original.postingDate(),
                JournalEntry.JournalStatus.DRAFT,
                lines,
                original.id(),
                0,
                Instant.now(clock),
                command.actor())
            .post();
    repository.saveJournal(reversal);
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
      final java.time.LocalDate date) {
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
    if (year.closed() || period.closed()) {
      throw new FinanceException("Closed accounting periods cannot accept postings.");
    }
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
        authorization.requireCostCenter(actor, line.costCenterId());
      }
      if (line.dimensionCode() != null) {
        authorization.requireDimension(actor, line.dimensionCode());
      }
    }
  }
}
