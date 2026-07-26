package com.newland.erp.logistics.api;

import com.newland.erp.logistics.application.LogisticsSecurityPort;
import com.newland.erp.logistics.application.LogisticsService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/import-logistics")
public final class LogisticsController {
  private final LogisticsService service;
  private final LogisticsSecurityPort security;

  public LogisticsController(
      final LogisticsService logisticsService, final LogisticsSecurityPort securityPort) {
    service = logisticsService;
    security = securityPort;
  }

  @PostMapping("/shipments")
  @ResponseStatus(HttpStatus.CREATED)
  public LogisticsDtos.ShipmentResponse create(
      @Valid @RequestBody final LogisticsDtos.CreateShipmentRequest request) {
    return LogisticsDtos.ShipmentResponse.from(
        service.createShipment(request.command(security.currentActor())));
  }

  @GetMapping("/shipments")
  public List<LogisticsDtos.ShipmentResponse> list(@RequestParam final UUID companyId) {
    return service.shipments(companyId, security.currentActor()).stream()
        .map(LogisticsDtos.ShipmentResponse::from)
        .toList();
  }

  @PostMapping("/shipments/{shipmentId}/book")
  public LogisticsDtos.ShipmentResponse book(@PathVariable final UUID shipmentId) {
    return LogisticsDtos.ShipmentResponse.from(
        service.book(shipmentId, security.currentActor()));
  }

  @PostMapping("/shipments/{shipmentId}/containers")
  public LogisticsDtos.ShipmentResponse addContainer(
      @PathVariable final UUID shipmentId,
      @Valid @RequestBody final LogisticsDtos.AddContainerRequest request) {
    return LogisticsDtos.ShipmentResponse.from(
        service.addContainer(shipmentId, request.domain(), security.currentActor()));
  }

  @PostMapping("/shipments/{shipmentId}/containers/{containerId}/load")
  public LogisticsDtos.ShipmentResponse loadContainer(
      @PathVariable final UUID shipmentId, @PathVariable final UUID containerId) {
    return LogisticsDtos.ShipmentResponse.from(
        service.loadContainer(shipmentId, containerId, security.currentActor()));
  }

  @PostMapping("/shipments/{shipmentId}/customs-milestones")
  public LogisticsDtos.ShipmentResponse milestone(
      @PathVariable final UUID shipmentId,
      @Valid @RequestBody final LogisticsDtos.MilestoneRequest request) {
    return LogisticsDtos.ShipmentResponse.from(
        service.recordMilestone(shipmentId, request.domain(), security.currentActor()));
  }

  @PostMapping("/shipments/{shipmentId}/landed-cost-drafts")
  public LogisticsDtos.LandedCostResponse landedCost(
      @PathVariable final UUID shipmentId,
      @Valid @RequestBody final LogisticsDtos.LandedCostRequest request) {
    return LogisticsDtos.LandedCostResponse.from(
        service.createLandedCostDraft(request.domain(shipmentId, security.currentActor())));
  }
}
