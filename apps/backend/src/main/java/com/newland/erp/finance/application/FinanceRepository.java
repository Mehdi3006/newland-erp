package com.newland.erp.finance.application;

import com.newland.erp.finance.domain.Account;
import com.newland.erp.finance.domain.AccountingPeriod;
import com.newland.erp.finance.domain.FiscalYear;
import com.newland.erp.finance.domain.JournalEntry;
import com.newland.erp.finance.domain.JournalReversal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FinanceRepository {
  boolean idempotencyKeyExists(String key);

  boolean accountCodeExists(UUID companyId, String code);

  List<Account> accounts(UUID companyId);

  Account saveAccount(Account account);

  FiscalYear saveFiscalYear(FiscalYear fiscalYear);

  AccountingPeriod savePeriod(AccountingPeriod period);

  Optional<FiscalYear> findFiscalYear(UUID id);

  Optional<AccountingPeriod> findPeriod(UUID id);

  JournalEntry saveJournal(JournalEntry journal);

  Optional<JournalEntry> findJournal(UUID id);

  boolean reversalExists(UUID journalId);

  JournalReversal saveReversal(JournalReversal reversal);
}
