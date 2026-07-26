package com.newland.erp.finance.application;

import com.newland.erp.finance.domain.Account;
import com.newland.erp.finance.domain.AccountingPeriod;
import com.newland.erp.finance.domain.AccountingPeriodContract;
import com.newland.erp.finance.domain.JournalEntry;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class FinanceCommands {
  public record CreateAccount(
      UUID companyId,
      String code,
      String name,
      Account.AccountType type,
      UUID parentId,
      boolean postable,
      String actor) {}

  public record CreateFiscalYear(
      UUID companyId,
      String code,
      LocalDate startsOn,
      LocalDate endsOn,
      boolean closed,
      String actor) {}

  public record CreatePeriod(
      UUID fiscalYearId,
      String code,
      LocalDate startsOn,
      LocalDate endsOn,
      boolean closed,
      String actor) {}

  public record TransitionPeriod(
      UUID periodId, AccountingPeriod.State targetState, String actor) {}

  public record CreateJournal(
      String idempotencyKey,
      UUID companyId,
      UUID branchId,
      UUID fiscalYearId,
      UUID periodId,
      LocalDate postingDate,
      AccountingPeriodContract.PostingPurpose postingPurpose,
      List<JournalEntry.JournalLine> lines,
      List<UUID> attachmentIds,
      String actor) {}

  public record EditJournal(UUID journalId, List<JournalEntry.JournalLine> lines, String actor) {}

  public record PostJournal(
      UUID journalId,
      AccountingPeriodContract.PostingPurpose postingPurpose,
      java.util.Map<String, String> taxContext,
      String actor) {}

  public record PostJournalWithSnapshot(
      UUID journalId,
      AccountingPeriodContract.PostingPurpose postingPurpose,
      com.newland.erp.finance.domain.JournalPostingSnapshot snapshot,
      String actor) {}

  public record ReverseJournal(
      UUID journalId,
      String idempotencyKey,
      LocalDate postingDate,
      AccountingPeriodContract.PostingPurpose postingPurpose,
      String actor) {}

  private FinanceCommands() {}
}
