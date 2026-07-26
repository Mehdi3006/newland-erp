package com.newland.erp.finance.application.integration;

import com.newland.erp.finance.domain.FinancialDocumentNumber;

/** Published atomic numbering boundary for future Finance-owned documents. */
public interface FinancialDocumentNumberPort {
  FinancialDocumentNumber.Assignment assign(FinancialDocumentNumber.Request request);
}
