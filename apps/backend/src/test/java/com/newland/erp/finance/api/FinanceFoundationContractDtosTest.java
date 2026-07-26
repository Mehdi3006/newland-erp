package com.newland.erp.finance.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.newland.erp.finance.domain.AccountingPeriodContract;
import com.newland.erp.finance.domain.FinancialDocumentNumber;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class FinanceFoundationContractDtosTest {
  @Test
  void mapsContractsWithoutExposingPersistenceTypes() {
    final UUID companyId = UUID.randomUUID();
    final UUID fiscalYearId = UUID.randomUUID();
    final UUID documentId = UUID.randomUUID();
    final var period =
        new AccountingPeriodContract(
            companyId,
            fiscalYearId,
            UUID.randomUUID(),
            "P01",
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 31),
            AccountingPeriodContract.State.OPEN);
    final var number =
        new FinancialDocumentNumber.Assignment(
            "COMP-JV-2026-1",
            "JV",
            companyId,
            fiscalYearId,
            documentId,
            Instant.parse("2026-01-31T00:00:00Z"));

    assertThat(FinanceFoundationContractDtos.AccountingPeriodResponse.from(period).state())
        .isEqualTo("OPEN");
    assertThat(FinanceFoundationContractDtos.DocumentNumberResponse.from(number).documentId())
        .isEqualTo(documentId);
  }
}
