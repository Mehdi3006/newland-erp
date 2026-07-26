package com.newland.erp.finance.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class FinanceFoundationContractTest {
  private static final UUID COMPANY_ID = UUID.randomUUID();

  @Test
  void periodContractEnforcesDateStateAndPostingPurpose() {
    final AccountingPeriodContract open = period(AccountingPeriodContract.State.OPEN);
    open.requirePostingAllowed(
        LocalDate.of(2026, 6, 15), AccountingPeriodContract.PostingPurpose.ORDINARY);

    final AccountingPeriodContract closing = period(AccountingPeriodContract.State.CLOSING);
    closing.requirePostingAllowed(
        LocalDate.of(2026, 6, 15),
        AccountingPeriodContract.PostingPurpose.CLOSE_ADJUSTMENT);
    assertThatThrownBy(
            () ->
                closing.requirePostingAllowed(
                    LocalDate.of(2026, 6, 15),
                    AccountingPeriodContract.PostingPurpose.ORDINARY))
        .isInstanceOf(FinanceException.class);

    assertThatThrownBy(
            () ->
                period(AccountingPeriodContract.State.CLOSED)
                    .requirePostingAllowed(
                        LocalDate.of(2026, 6, 15),
                        AccountingPeriodContract.PostingPurpose.CLOSE_ADJUSTMENT))
        .isInstanceOf(FinanceException.class);
  }

  @Test
  void journalContractRequiresBalancedImmutableLinesAndCurrencySnapshots() {
    final List<JournalEntryContract.Line> mutable =
        new java.util.ArrayList<>(
            List.of(line("100.00", "0"), line("0", "100.00")));
    final JournalEntryContract journal = journal(mutable);
    mutable.clear();

    assertThat(journal.lines()).hasSize(2);
    assertThat(journal.lines().getFirst().transactionCurrency()).isEqualTo("USD");
    assertThat(journal.lines().getFirst().exchangeRateSnapshot())
        .isEqualByComparingTo("1.000000000000");

    assertThatThrownBy(() -> journal(List.of(line("100", "0"), line("0", "99"))))
        .isInstanceOf(FinanceException.class)
        .hasMessageContaining("debit must equal credit");
  }

  @Test
  void currencyRateSnapshotRejectsMismatchedCompanyCurrencyAndValidity() {
    final CurrencyExchangeContract.RateSnapshot snapshot =
        new CurrencyExchangeContract.RateSnapshot(
            UUID.randomUUID(),
            COMPANY_ID,
            "eur",
            "usd",
            "spot",
            "approved-source",
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 12, 31),
            new BigDecimal("1.1234567890123"));
    snapshot.requireMatches(
        new CurrencyExchangeContract.RateQuery(
            COMPANY_ID, "EUR", "USD", "SPOT", LocalDate.of(2026, 6, 1)));

    assertThat(snapshot.rate()).isEqualByComparingTo("1.123456789012");
    assertThatThrownBy(
            () ->
                snapshot.requireMatches(
                    new CurrencyExchangeContract.RateQuery(
                        UUID.randomUUID(),
                        "EUR",
                        "USD",
                        "SPOT",
                        LocalDate.of(2026, 6, 1))))
        .isInstanceOf(FinanceException.class);
  }

  @Test
  void financialNumberContractsNormalizeTypeAndRequireAssignmentMetadata() {
    final UUID fiscalYearId = UUID.randomUUID();
    final UUID documentId = UUID.randomUUID();
    final FinancialDocumentNumber.Request request =
        new FinancialDocumentNumber.Request(
            "jv", COMPANY_ID, null, fiscalYearId, documentId, "journal-1");
    final FinancialDocumentNumber.Assignment assignment =
        new FinancialDocumentNumber.Assignment(
            "COMP-JV-2026-000001",
            request.documentType(),
            request.companyId(),
            request.fiscalYearId(),
            request.documentId(),
            Instant.parse("2026-07-26T00:00:00Z"));

    assertThat(request.documentType()).isEqualTo("JV");
    assertThat(assignment.documentId()).isEqualTo(documentId);
  }

  private static AccountingPeriodContract period(final AccountingPeriodContract.State state) {
    return new AccountingPeriodContract(
        COMPANY_ID,
        UUID.randomUUID(),
        UUID.randomUUID(),
        "2026-06",
        LocalDate.of(2026, 6, 1),
        LocalDate.of(2026, 6, 30),
        state);
  }

  private static JournalEntryContract journal(final List<JournalEntryContract.Line> lines) {
    return new JournalEntryContract(
        UUID.randomUUID(),
        "JV-1",
        "journal-key",
        COMPANY_ID,
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        LocalDate.of(2026, 6, 15),
        JournalEntryContract.Status.POSTED,
        "SOURCE",
        UUID.randomUUID(),
        null,
        lines,
        0);
  }

  private static JournalEntryContract.Line line(final String debit, final String credit) {
    return new JournalEntryContract.Line(
        UUID.randomUUID(),
        UUID.randomUUID(),
        new BigDecimal(debit),
        new BigDecimal(credit),
        null,
        null,
        "",
        "usd",
        new BigDecimal(debit).max(new BigDecimal(credit)),
        BigDecimal.ONE);
  }
}
