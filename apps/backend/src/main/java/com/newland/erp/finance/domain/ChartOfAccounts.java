package com.newland.erp.finance.domain;

import java.util.Collection;
import java.util.UUID;

public record ChartOfAccounts(UUID id, UUID companyId, String name) {
  public ChartOfAccounts {
    if (id == null || companyId == null || name == null || name.isBlank()) {
      throw new IllegalArgumentException("Chart of accounts is required.");
    }
  }

  public static void rejectCycle(
      final UUID accountId, final UUID parentId, final Collection<Account> accounts) {
    UUID cursor = parentId;
    while (cursor != null) {
      if (cursor.equals(accountId)) {
        throw new FinanceException("Account hierarchy cannot contain a cycle.");
      }
      final UUID current = cursor;
      cursor =
          accounts.stream()
              .filter(a -> a.id().equals(current))
              .findFirst()
              .map(Account::parentId)
              .orElse(null);
    }
  }
}
