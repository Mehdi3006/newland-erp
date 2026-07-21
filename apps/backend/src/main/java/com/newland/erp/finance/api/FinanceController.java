package com.newland.erp.finance.api;

import com.newland.erp.finance.application.FinanceCommands;
import com.newland.erp.finance.application.FinanceService;
import com.newland.erp.finance.domain.Account;
import com.newland.erp.finance.domain.AccountingPeriod;
import com.newland.erp.finance.domain.FiscalYear;
import com.newland.erp.finance.domain.JournalEntry;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/finance")
public final class FinanceController {
  private final FinanceService service;

  public FinanceController(final FinanceService financeService) {
    this.service = financeService;
  }

  @PostMapping("/accounts")
  @ResponseStatus(HttpStatus.CREATED)
  public Account account(
      @Valid @RequestBody final AccountRequest r,
      @RequestHeader(name = "X-Newland-Actor", defaultValue = "system") final String actor) {
    return service.createAccount(
        new FinanceCommands.CreateAccount(
            r.companyId(), r.code(), r.name(), r.type(), r.parentId(), r.postable(), actor));
  }

  @PostMapping("/fiscal-years")
  public FiscalYear fiscalYear(
      @Valid @RequestBody final FiscalYearRequest r,
      @RequestHeader(name = "X-Newland-Actor", defaultValue = "system") final String actor) {
    return service.createFiscalYear(
        new FinanceCommands.CreateFiscalYear(
            r.companyId(), r.code(), r.startsOn(), r.endsOn(), r.closed(), actor));
  }

  @PostMapping("/periods")
  public AccountingPeriod period(
      @Valid @RequestBody final PeriodRequest r,
      @RequestHeader(name = "X-Newland-Actor", defaultValue = "system") final String actor) {
    return service.createPeriod(
        new FinanceCommands.CreatePeriod(
            r.fiscalYearId(), r.code(), r.startsOn(), r.endsOn(), r.closed(), actor));
  }

  @PostMapping("/journals")
  @ResponseStatus(HttpStatus.CREATED)
  public JournalEntry journal(
      @Valid @RequestBody final JournalRequest r,
      @RequestHeader(name = "X-Newland-Actor", defaultValue = "system") final String actor) {
    return service.createJournal(
        new FinanceCommands.CreateJournal(
            r.idempotencyKey(),
            r.companyId(),
            r.branchId(),
            r.fiscalYearId(),
            r.periodId(),
            r.postingDate(),
            r.lines().stream().map(LineRequest::toDomain).toList(),
            r.attachmentIds() == null ? List.of() : r.attachmentIds(),
            actor));
  }

  @PostMapping("/journals/{id}/post")
  public JournalEntry post(
      @PathVariable final UUID id,
      @RequestHeader(name = "X-Newland-Actor", defaultValue = "system") final String actor) {
    return service.postJournal(new FinanceCommands.PostJournal(id, actor));
  }

  @PostMapping("/journals/{id}/reverse")
  public JournalEntry reverse(
      @PathVariable final UUID id,
      @RequestBody final IdempotencyRequest request,
      @RequestHeader(name = "X-Newland-Actor", defaultValue = "system") final String actor) {
    return service.reverseJournal(
        new FinanceCommands.ReverseJournal(id, request.idempotencyKey(), actor));
  }

  public record AccountRequest(
      @NotNull UUID companyId,
      @NotBlank String code,
      @NotBlank String name,
      @NotNull Account.AccountType type,
      UUID parentId,
      boolean postable) {}

  public record FiscalYearRequest(
      @NotNull UUID companyId,
      @NotBlank String code,
      @NotNull LocalDate startsOn,
      @NotNull LocalDate endsOn,
      boolean closed) {}

  public record PeriodRequest(
      @NotNull UUID fiscalYearId,
      @NotBlank String code,
      @NotNull LocalDate startsOn,
      @NotNull LocalDate endsOn,
      boolean closed) {}

  public record JournalRequest(
      @NotBlank String idempotencyKey,
      @NotNull UUID companyId,
      @NotNull UUID branchId,
      @NotNull UUID fiscalYearId,
      @NotNull UUID periodId,
      @NotNull LocalDate postingDate,
      @NotEmpty List<@Valid LineRequest> lines,
      List<UUID> attachmentIds) {}

  public record LineRequest(
      @NotNull UUID accountId,
      @NotNull BigDecimal debit,
      @NotNull BigDecimal credit,
      UUID costCenterId,
      UUID profitCenterId,
      String dimensionCode,
      UUID currencyId,
      BigDecimal currencyAmount,
      BigDecimal exchangeRateSnapshot) {
    JournalEntry.JournalLine toDomain() {
      return new JournalEntry.JournalLine(
          UUID.randomUUID(),
          accountId,
          debit,
          credit,
          costCenterId,
          profitCenterId,
          dimensionCode,
          currencyId,
          currencyAmount,
          exchangeRateSnapshot);
    }
  }

  public record IdempotencyRequest(@NotBlank String idempotencyKey) {}
}
