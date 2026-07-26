package com.newland.erp.finance.application.integration;

import com.newland.erp.finance.domain.AccountingPeriodContract;
import java.time.LocalDate;
import java.util.UUID;

/** Published Finance period resolution boundary for future financial subledgers. */
public interface AccountingPeriodContractPort {
  AccountingPeriodContract requirePostingPeriod(
      UUID companyId, LocalDate postingDate, AccountingPeriodContract.PostingPurpose purpose);
}
