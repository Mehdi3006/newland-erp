package com.newland.erp.finance.domain;

import java.util.UUID;

public record Account(
    UUID id,
    UUID companyId,
    String code,
    String name,
    AccountType type,
    UUID parentId,
    boolean postable,
    boolean active) {
  public Account {
    if (id == null
        || companyId == null
        || code == null
        || code.isBlank()
        || name == null
        || name.isBlank()
        || type == null) {
      throw new IllegalArgumentException("Account identity, code, name, and type are required.");
    }
    code = code.trim().toUpperCase();
  }

  public void requirePostable() {
    if (!postable) {
      throw new FinanceException("Posting to a parent or non-postable account is forbidden.");
    }
    if (!active) {
      throw new FinanceException("Posting to an inactive or blocked account is forbidden.");
    }
  }

  public enum AccountType {
    ASSET,
    LIABILITY,
    EQUITY,
    REVENUE,
    EXPENSE
  }
}
