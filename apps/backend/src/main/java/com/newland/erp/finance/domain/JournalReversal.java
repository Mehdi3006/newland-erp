package com.newland.erp.finance.domain;

import java.util.UUID;

public record JournalReversal(UUID id, UUID originalJournalId, UUID reversalJournalId) {
  public JournalReversal {
    if (id == null || originalJournalId == null || reversalJournalId == null) {
      throw new IllegalArgumentException("Journal reversal references are required.");
    }
  }
}
