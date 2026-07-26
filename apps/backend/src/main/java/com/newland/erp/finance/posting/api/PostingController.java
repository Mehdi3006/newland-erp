package com.newland.erp.finance.posting.api;

import com.newland.erp.finance.posting.application.FinancialPostingPort;
import com.newland.erp.finance.posting.application.PostingService;
import com.newland.erp.finance.posting.api.PostingApiDtos.PostingRequestResponse;
import com.newland.erp.finance.posting.api.PostingApiDtos.PostingResultResponse;
import com.newland.erp.finance.posting.domain.AccountingEvent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance/posting")
public final class PostingController {
  private final FinancialPostingPort posting;
  private final com.newland.erp.finance.posting.application.PostingPorts.CurrentUserPort users;

  public PostingController(
      final PostingService postingService,
      final com.newland.erp.finance.posting.application.PostingPorts.CurrentUserPort currentUsers) {
    posting = postingService;
    users = currentUsers;
  }

  @PostMapping("/events")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public PostingResultResponse submit(@Valid @RequestBody final EventRequest request) {
    return PostingResultResponse.from(posting.submit(request.toDomain(users.currentUser())));
  }

  @PostMapping("/events/preview")
  public PostingResultResponse preview(@Valid @RequestBody final EventRequest request) {
    return PostingResultResponse.from(posting.preview(request.toDomain(users.currentUser())));
  }

  @GetMapping("/requests/{id}")
  public PostingRequestResponse status(@PathVariable final UUID id) {
    return PostingRequestResponse.from(posting.status(id));
  }

  @PostMapping("/requests/{id}/retry")
  public PostingResultResponse retry(@PathVariable final UUID id) {
    return PostingResultResponse.from(posting.retry(id));
  }

  public record EventRequest(
      @NotNull UUID eventId,
      @NotBlank String idempotencyKey,
      @NotBlank String eventType,
      @NotBlank String sourceModule,
      @NotBlank String sourceDocumentType,
      @NotNull UUID sourceDocumentId,
      String sourceDocumentNumber,
      @NotNull UUID companyId,
      @NotNull UUID branchId,
      @NotNull LocalDate eventDate,
      @NotNull LocalDate accountingDate,
      @NotBlank String currencyCode,
      @NotNull BigDecimal exchangeRate,
      @NotNull BigDecimal amount,
      BigDecimal taxAmount,
      BigDecimal netAmount,
      String description,
      Map<String, String> dimensions,
      Map<String, String> attributes) {
    AccountingEvent toDomain(final String actor) {
      return new AccountingEvent(
          eventId,
          idempotencyKey,
          eventType,
          sourceModule,
          sourceDocumentType,
          sourceDocumentId,
          sourceDocumentNumber,
          companyId,
          branchId,
          eventDate,
          accountingDate,
          currencyCode,
          exchangeRate,
          amount,
          taxAmount,
          netAmount,
          description,
          dimensions,
          attributes,
          Instant.now(),
          actor,
          1);
    }
  }
}
