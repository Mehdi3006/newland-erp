package com.newland.erp.finance.application;

import com.newland.erp.finance.domain.Account;
import com.newland.erp.finance.domain.AccountingPeriod;
import com.newland.erp.finance.domain.CostCenter;
import com.newland.erp.finance.domain.FiscalYear;
import com.newland.erp.finance.domain.JournalEntry;
import com.newland.erp.finance.domain.JournalReversal;
import com.newland.erp.finance.domain.ProfitCenter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FinanceRepository {
  boolean idempotencyKeyExists(String key);

  boolean accountCodeExists(UUID companyId, String code);

  List<Account> accounts(UUID companyId);

  Optional<Account> findAccount(UUID companyId, UUID accountId);

  Optional<CostCenter> findCostCenter(UUID companyId, UUID costCenterId);

  Optional<ProfitCenter> findProfitCenter(UUID companyId, UUID profitCenterId);

  boolean financialDimensionIsActive(UUID companyId, String dimensionCode);

  Account saveAccount(Account account);

  FiscalYear saveFiscalYear(FiscalYear fiscalYear);

  AccountingPeriod savePeriod(AccountingPeriod period);

  Optional<FiscalYear> findFiscalYear(UUID id);

  Optional<AccountingPeriod> findPeriod(UUID id);

  Optional<PostingPeriod> findOpenPostingPeriod(UUID companyId, java.time.LocalDate postingDate);

  JournalEntry saveJournal(JournalEntry journal);

  Optional<JournalEntry> findJournal(UUID id);

  Optional<JournalEntry> findJournalByIdempotencyKey(String idempotencyKey);

  boolean reversalExists(UUID journalId);

  JournalReversal saveReversal(JournalReversal reversal);

  record PostingPeriod(UUID fiscalYearId, UUID periodId) {}
}
