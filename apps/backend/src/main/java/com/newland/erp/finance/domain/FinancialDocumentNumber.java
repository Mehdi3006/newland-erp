package com.newland.erp.finance.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/** Atomic number-series request and immutable assignment for a posted financial document. */
public final class FinancialDocumentNumber {
  public record Request(
      String documentType,
      UUID companyId,
      UUID branchId,
      UUID fiscalYearId,
      UUID documentId,
      String idempotencyKey) {
    public Request {
      documentType =
          AccountingPeriodContract.text(documentType, "document type").toUpperCase(Locale.ROOT);
      AccountingPeriodContract.required(companyId, "company id");
      AccountingPeriodContract.required(fiscalYearId, "fiscal year id");
      AccountingPeriodContract.required(documentId, "document id");
      idempotencyKey = AccountingPeriodContract.text(idempotencyKey, "idempotency key");
    }
  }

  public record Assignment(
      String number,
      String documentType,
      UUID companyId,
      UUID fiscalYearId,
      UUID documentId,
      Instant assignedAt) {
    public Assignment {
      number = AccountingPeriodContract.text(number, "financial document number");
      documentType =
          AccountingPeriodContract.text(documentType, "document type").toUpperCase(Locale.ROOT);
      AccountingPeriodContract.required(companyId, "company id");
      AccountingPeriodContract.required(fiscalYearId, "fiscal year id");
      AccountingPeriodContract.required(documentId, "document id");
      AccountingPeriodContract.required(assignedAt, "number assignment time");
    }
  }

  private FinancialDocumentNumber() {}
}
