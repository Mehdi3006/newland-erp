package com.newland.erp.crm.api;

import com.newland.erp.crm.application.CrmSecurityPort;
import com.newland.erp.crm.application.CrmService;
import com.newland.erp.crm.domain.Opportunity;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/v1/crm")
public final class CrmController {
  private final CrmService service;
  private final CrmSecurityPort security;

  public CrmController(final CrmService crmService, final CrmSecurityPort securityPort) {
    service = crmService;
    security = securityPort;
  }

  @PostMapping("/leads")
  @ResponseStatus(HttpStatus.CREATED)
  public CrmDtos.LeadResponse createLead(
      @Valid @RequestBody final CrmDtos.CreateLeadRequest request) {
    return CrmDtos.LeadResponse.from(service.createLead(request.domain(security.currentActor())));
  }

  @PutMapping("/leads/{leadId}/qualify")
  public CrmDtos.LeadResponse qualifyLead(@PathVariable final UUID leadId) {
    return CrmDtos.LeadResponse.from(service.qualifyLead(leadId, security.currentActor()));
  }

  @PutMapping("/leads/{leadId}/disqualify")
  public CrmDtos.LeadResponse disqualifyLead(
      @PathVariable final UUID leadId,
      @Valid @RequestBody final CrmDtos.ReasonRequest request) {
    return CrmDtos.LeadResponse.from(
        service.disqualifyLead(leadId, request.reason(), security.currentActor()));
  }

  @PostMapping("/opportunities")
  @ResponseStatus(HttpStatus.CREATED)
  public CrmDtos.OpportunityResponse createOpportunity(
      @Valid @RequestBody final CrmDtos.CreateOpportunityRequest request) {
    return CrmDtos.OpportunityResponse.from(
        service.createOpportunity(request.domain(security.currentActor())));
  }

  @PutMapping("/opportunities/{opportunityId}/stage")
  public CrmDtos.OpportunityResponse advanceOpportunity(
      @PathVariable final UUID opportunityId,
      @Valid @RequestBody final CrmDtos.StageRequest request) {
    return CrmDtos.OpportunityResponse.from(
        service.advanceOpportunity(
            opportunityId,
            Opportunity.Stage.valueOf(request.stage().toUpperCase(java.util.Locale.ROOT)),
            request.reason(),
            security.currentActor()));
  }

  @PostMapping("/activities")
  @ResponseStatus(HttpStatus.CREATED)
  public CrmDtos.ActivityResponse recordActivity(
      @Valid @RequestBody final CrmDtos.CreateActivityRequest request) {
    return CrmDtos.ActivityResponse.from(
        service.recordActivity(request.domain(security.currentActor())));
  }

  @GetMapping("/customers/{customerId}/timeline")
  public List<CrmDtos.ActivityResponse> timeline(
      @PathVariable final UUID customerId, @RequestParam final UUID companyId) {
    return service.customerTimeline(companyId, customerId, security.currentActor()).stream()
        .map(CrmDtos.ActivityResponse::from)
        .toList();
  }
}
