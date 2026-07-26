package com.newland.erp.finance.application;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.newland.erp.finance.domain.FinanceException;
import com.newland.erp.finance.domain.JournalEntry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class FinanceServicePostingIdempotencyTest {
  @Test
  void returnsTheExistingPostedJournalForAnIdenticalIdempotentRetry() {
    final FinanceRepository repository = mock(FinanceRepository.class);
    final FinancePorts.AuthorizationPort authorization = mock(FinancePorts.AuthorizationPort.class);
    final JournalEntry journal = journal();
    when(repository.findJournalByIdempotencyKey(journal.idempotencyKey()))
        .thenReturn(Optional.of(journal));
    final FinanceService service = service(repository, authorization);

    final JournalEntry retried =
        service.createAndPostJournal(
            journal.idempotencyKey(),
            journal.companyId(),
            journal.branchId(),
            journal.postingDate(),
            journal.lines(),
            journal.actor());

    assertSame(journal, retried);
    verify(authorization).require(journal.actor(), "finance.journal.post", journal.companyId());
    verify(repository, never()).saveJournal(any());
  }

  @Test
  void rejectsAnIdempotencyKeyReusedWithDifferentJournalData() {
    final FinanceRepository repository = mock(FinanceRepository.class);
    final JournalEntry journal = journal();
    when(repository.findJournalByIdempotencyKey(journal.idempotencyKey()))
        .thenReturn(Optional.of(journal));
    final FinanceService service =
        service(repository, mock(FinancePorts.AuthorizationPort.class));

    assertThrows(
        FinanceException.class,
        () ->
            service.createAndPostJournal(
                journal.idempotencyKey(),
                journal.companyId(),
                UUID.randomUUID(),
                journal.postingDate(),
                journal.lines(),
                journal.actor()));
  }

  private static FinanceService service(
      final FinanceRepository repository,
      final FinancePorts.AuthorizationPort authorization) {
    return new FinanceService(
        repository,
        mock(FinancePorts.EnterprisePort.class),
        mock(FinancePorts.MasterDataPort.class),
        authorization,
        mock(FinancePorts.NumberSeriesPort.class),
        mock(FinancePorts.AuditPort.class),
        mock(FinancePorts.OutboxPort.class),
        mock(FinancePorts.AttachmentPort.class),
        Clock.fixed(Instant.parse("2026-07-22T00:00:00Z"), ZoneOffset.UTC));
  }

  private static JournalEntry journal() {
    final UUID companyId = UUID.randomUUID();
    final UUID debitAccount = UUID.randomUUID();
    final UUID creditAccount = UUID.randomUUID();
    final BigDecimal amount = new BigDecimal("100.00");
    return new JournalEntry(
        UUID.randomUUID(),
        "JE-1",
        "posting:" + UUID.randomUUID(),
        companyId,
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        LocalDate.of(2026, 7, 22),
        JournalEntry.JournalStatus.POSTED,
        List.of(
            new JournalEntry.JournalLine(
                UUID.randomUUID(),
                debitAccount,
                amount,
                BigDecimal.ZERO,
                null,
                null,
                null,
                null,
                amount,
                BigDecimal.ONE),
            new JournalEntry.JournalLine(
                UUID.randomUUID(),
                creditAccount,
                BigDecimal.ZERO,
                amount,
                null,
                null,
                null,
                null,
                amount,
                BigDecimal.ONE)),
        null,
        1,
        Instant.parse("2026-07-22T00:00:00Z"),
        UUID.randomUUID().toString());
  }
}
