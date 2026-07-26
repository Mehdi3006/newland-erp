package com.newland.erp.servicewarranty.api;

import com.newland.erp.servicewarranty.application.ServiceWarrantySecurityPort;
import com.newland.erp.servicewarranty.application.ServiceWarrantyService;
import com.newland.erp.servicewarranty.domain.ServiceTicket;
import jakarta.validation.Valid;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/service-warranty")
public final class ServiceWarrantyController {
  private final ServiceWarrantyService service;
  private final ServiceWarrantySecurityPort security;

  public ServiceWarrantyController(
      final ServiceWarrantyService warrantyService,
      final ServiceWarrantySecurityPort securityPort) {
    service = warrantyService;
    security = securityPort;
  }

  @PostMapping("/policies")
  @ResponseStatus(HttpStatus.CREATED)
  public ServiceWarrantyDtos.PolicyResponse createPolicy(
      @Valid @RequestBody final ServiceWarrantyDtos.CreatePolicyRequest request) {
    return ServiceWarrantyDtos.PolicyResponse.from(
        service.createPolicy(request.domain(), security.currentActor()));
  }

  @PostMapping("/tickets")
  @ResponseStatus(HttpStatus.CREATED)
  public ServiceWarrantyDtos.TicketResponse createTicket(
      @Valid @RequestBody final ServiceWarrantyDtos.CreateTicketRequest request) {
    return ServiceWarrantyDtos.TicketResponse.from(
        service.createTicket(request.domain(security.currentActor())));
  }

  @GetMapping("/tickets/{ticketId}")
  public ServiceWarrantyDtos.TicketResponse ticket(
      @PathVariable final UUID ticketId, @RequestParam final UUID companyId) {
    final String actor = security.currentActor();
    security.require(actor, "service.ticket.manage", companyId);
    final ServiceTicket ticket = service.ticket(ticketId, companyId);
    return ServiceWarrantyDtos.TicketResponse.from(ticket);
  }

  @PutMapping("/tickets/{ticketId}/validate-warranty")
  public ServiceWarrantyDtos.TicketResponse validate(@PathVariable final UUID ticketId) {
    return ServiceWarrantyDtos.TicketResponse.from(
        service.validateWarranty(ticketId, security.currentActor()));
  }

  @PutMapping("/tickets/{ticketId}/diagnosis")
  public ServiceWarrantyDtos.TicketResponse diagnose(
      @PathVariable final UUID ticketId,
      @Valid @RequestBody final ServiceWarrantyDtos.DiagnosisRequest request) {
    return ServiceWarrantyDtos.TicketResponse.from(
        service.diagnose(
            ticketId, request.findings(), request.recommendation(), security.currentActor()));
  }

  @PutMapping("/tickets/{ticketId}/resolution")
  public ServiceWarrantyDtos.TicketResponse resolution(
      @PathVariable final UUID ticketId,
      @Valid @RequestBody final ServiceWarrantyDtos.ResolutionRequest request) {
    return ServiceWarrantyDtos.TicketResponse.from(
        service.approveResolution(
            ticketId,
            ServiceTicket.Resolution.Type.valueOf(request.type().toUpperCase(Locale.ROOT)),
            request.notes(),
            security.currentActor()));
  }

  @PutMapping("/tickets/{ticketId}/close")
  public ServiceWarrantyDtos.TicketResponse close(
      @PathVariable final UUID ticketId,
      @Valid @RequestBody final ServiceWarrantyDtos.CloseRequest request) {
    return ServiceWarrantyDtos.TicketResponse.from(
        service.close(ticketId, request.outcome(), security.currentActor()));
  }
}
