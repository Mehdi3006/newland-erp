package com.newland.erp.procurement.api;

import com.newland.erp.procurement.application.ProcurementAccountingPorts;
import com.newland.erp.procurement.application.ProcurementAccountingService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/procurement/finance-postings")
public final class ProcurementAccountingController {
  private final ProcurementAccountingService service;
  private final ProcurementAccountingPorts.SecurityPort security;

  public ProcurementAccountingController(
      final ProcurementAccountingService accountingService,
      final ProcurementAccountingPorts.SecurityPort securityPort) {
    service = accountingService;
    security = securityPort;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.ACCEPTED)
  public ProcurementAccountingDtos.PostingResponse publish(
      @Valid @RequestBody final ProcurementAccountingDtos.PublishRequest request) {
    return ProcurementAccountingDtos.PostingResponse.from(
        service.publish(request.toDomain(security.currentActor())));
  }

  @PostMapping("/{accountingEventId}/retry")
  public ProcurementAccountingDtos.PostingResponse retry(
      @PathVariable final UUID accountingEventId,
      @Valid @RequestBody final ProcurementAccountingDtos.RetryRequest request) {
    return ProcurementAccountingDtos.PostingResponse.from(
        service.retry(accountingEventId, request.companyId(), security.currentActor()));
  }
}
