package com.newland.erp.finance.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class GeneralLedgerCoreTest {
  @Test
  void enforcesOpenClosingAndClosedPeriodPostingRules() {
    final AccountingPeriod open = period(AccountingPeriod.State.OPEN);
    open.requirePostingAllowed(
        LocalDate.of(2026, 7, 15), AccountingPeriodContract.PostingPurpose.ORDINARY);

    final AccountingPeriod closing = open.transitionTo(AccountingPeriod.State.CLOSING);
    closing.requirePostingAllowed(
        LocalDate.of(2026, 7, 15),
        AccountingPeriodContract.PostingPurpose.CLOSE_ADJUSTMENT);
    assertThatThrownBy(
            () ->
                closing.requirePostingAllowed(
                    LocalDate.of(2026, 7, 15),
                    AccountingPeriodContract.PostingPurpose.ORDINARY))
        .isInstanceOf(FinanceException.class);

    final AccountingPeriod closed = closing.transitionTo(AccountingPeriod.State.CLOSED);
    assertThatThrownBy(
            () ->
                closed.requirePostingAllowed(
                    LocalDate.of(2026, 7, 15),
                    AccountingPeriodContract.PostingPurpose.CLOSE_ADJUSTMENT))
        .isInstanceOf(FinanceException.class);
    assertThat(closed.transitionTo(AccountingPeriod.State.CLOSING).state())
        .isEqualTo(AccountingPeriod.State.CLOSING);
  }

  @Test
  void rejectsInvalidPeriodTransitions() {
    assertThatThrownBy(() -> period(AccountingPeriod.State.OPEN).transitionTo(
            AccountingPeriod.State.CLOSED))
        .isInstanceOf(FinanceException.class);
  }

  @Test
  void validatesImmutableCurrencyRateAndTaxSnapshot() {
    final Map<String, String> tax = new java.util.HashMap<>();
    tax.put("taxCategory", "STANDARD");
    final JournalPostingSnapshot snapshot =
        new JournalPostingSnapshot(
            UUID.randomUUID(),
            "eur",
            "usd",
            UUID.randomUUID(),
            "MASTER_DATA",
            "SPOT",
            LocalDate.of(2026, 7, 15),
            new BigDecimal("1.250000000000"),
            new BigDecimal("80.000000"),
            new BigDecimal("100.000000"),
            tax,
            java.time.Instant.parse("2026-07-15T00:00:00Z"));
    tax.clear();

    assertThat(snapshot.transactionCurrency()).isEqualTo("EUR");
    assertThat(snapshot.taxContext()).containsEntry("taxCategory", "STANDARD");
    assertThatThrownBy(
            () ->
                new JournalPostingSnapshot(
                    UUID.randomUUID(),
                    "USD",
                    "USD",
                    null,
                    "MASTER_DATA",
                    "SPOT",
                    LocalDate.of(2026, 7, 15),
                    new BigDecimal("1.100000000000"),
                    BigDecimal.TEN,
                    new BigDecimal("11"),
                    Map.of(),
                    java.time.Instant.parse("2026-07-15T00:00:00Z")))
        .isInstanceOf(FinanceException.class);
  }

  private static AccountingPeriod period(final AccountingPeriod.State state) {
    return new AccountingPeriod(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "2026-07",
        LocalDate.of(2026, 7, 1),
        LocalDate.of(2026, 7, 31),
        state);
  }
}
