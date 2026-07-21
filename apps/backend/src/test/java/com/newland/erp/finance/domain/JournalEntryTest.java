package com.newland.erp.finance.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class JournalEntryTest {
  private JournalEntry.JournalLine line(final BigDecimal debit, final BigDecimal credit) {
    return new JournalEntry.JournalLine(
        UUID.randomUUID(), UUID.randomUUID(), debit, credit, null, null, null, null, null, null);
  }

  private JournalEntry journal() {
    return new JournalEntry(
        UUID.randomUUID(),
        "JE-1",
        "key",
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        LocalDate.now(),
        JournalEntry.JournalStatus.DRAFT,
        List.of(line(BigDecimal.TEN, BigDecimal.ZERO), line(BigDecimal.ZERO, BigDecimal.TEN)),
        null,
        0,
        Instant.now(),
        "actor");
  }

  @Test
  void acceptsBalancedJournalAndMakesPostedImmutable() {
    assertEquals(JournalEntry.JournalStatus.POSTED, journal().post().status());
    assertThrows(FinanceException.class, () -> journal().post().revise(List.of()));
  }

  @Test
  void rejectsUnbalancedSingleAndZeroLines() {
    assertThrows(
        FinanceException.class,
        () ->
            new JournalEntry(
                UUID.randomUUID(),
                "JE",
                "k",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDate.now(),
                JournalEntry.JournalStatus.DRAFT,
                List.of(line(BigDecimal.TEN, BigDecimal.ZERO)),
                null,
                0,
                Instant.now(),
                "a"));
    assertThrows(FinanceException.class, () -> line(BigDecimal.ZERO, BigDecimal.ZERO));
  }

  @Test
  void rejectsAccountCyclesAndNonPostablePosting() {
    UUID id = UUID.randomUUID();
    Account a =
        new Account(
            id, UUID.randomUUID(), "100", "Parent", Account.AccountType.ASSET, null, false, true);
    assertThrows(FinanceException.class, () -> ChartOfAccounts.rejectCycle(id, id, List.of(a)));
    assertThrows(FinanceException.class, a::requirePostable);
  }
}
