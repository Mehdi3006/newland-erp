package com.newland.erp.finance.posting.application;

import com.newland.erp.finance.posting.domain.AccountingEvent;
import com.newland.erp.finance.posting.domain.PostingRequest;
import com.newland.erp.finance.posting.domain.PostingResult;
import java.util.UUID;

public interface FinancialPostingPort {
  PostingResult submit(AccountingEvent event);

  PostingResult preview(AccountingEvent event);

  PostingRequest status(UUID postingRequestId);

  PostingResult retry(UUID postingRequestId);
}
