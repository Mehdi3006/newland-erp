package com.newland.erp.finance.application.integration;

import com.newland.erp.finance.domain.JournalEntryContract;
import java.util.Optional;
import java.util.UUID;

/** Read-only published journal boundary for source reconciliation. */
public interface JournalEntryContractPort {
  Optional<JournalEntryContract> findBySource(
      UUID companyId, String sourceDocumentType, UUID sourceDocumentId);

  Optional<JournalEntryContract> findPostedJournal(UUID companyId, UUID journalEntryId);
}
