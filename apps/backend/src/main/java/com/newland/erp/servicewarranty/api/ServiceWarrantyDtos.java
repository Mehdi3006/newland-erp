package com.newland.erp.servicewarranty.api;

import com.newland.erp.servicewarranty.domain.ServiceTicket;
import com.newland.erp.servicewarranty.domain.WarrantyPolicy;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class ServiceWarrantyDtos {
  public record CreatePolicyRequest(
      @NotNull UUID companyId,
      UUID productId,
      @Min(1) int durationDays,
      boolean serialRequired,
      boolean salesEvidenceRequired,
      @NotNull LocalDate effectiveFrom,
      LocalDate effectiveTo,
      boolean active) {
    WarrantyPolicy domain() {
      return new WarrantyPolicy(
          UUID.randomUUID(), companyId, productId, durationDays, serialRequired,
          salesEvidenceRequired, effectiveFrom, effectiveTo, active);
    }
  }

  public record CreateTicketRequest(
      @NotBlank String idempotencyKey,
      @NotBlank String ticketNumber,
      @NotNull UUID companyId,
      UUID branchId,
      @NotNull UUID customerId,
      @NotNull UUID productId,
      @NotNull UUID skuId,
      String serialCode,
      UUID salesOrderId,
      LocalDate purchaseDate,
      @NotBlank String issueSummary) {
    ServiceTicket domain(final String actor) {
      final Instant now = Instant.now();
      return new ServiceTicket(
          UUID.randomUUID(), idempotencyKey, ticketNumber, companyId, branchId, customerId,
          productId, skuId, serialCode, salesOrderId, purchaseDate, issueSummary,
          ServiceTicket.Status.OPEN, null, null, null, 0, now, now, actor);
    }
  }

  public record DiagnosisRequest(
      @NotBlank String findings, @NotBlank String recommendation) {}

  public record ResolutionRequest(
      @NotBlank String type, @NotBlank String notes) {}

  public record CloseRequest(@NotBlank String outcome) {}

  public record PolicyResponse(
      UUID id,
      UUID companyId,
      UUID productId,
      int durationDays,
      boolean serialRequired,
      boolean salesEvidenceRequired,
      LocalDate effectiveFrom,
      LocalDate effectiveTo,
      boolean active) {
    static PolicyResponse from(final WarrantyPolicy policy) {
      return new PolicyResponse(
          policy.id(), policy.companyId(), policy.productId(), policy.durationDays(),
          policy.serialRequired(), policy.salesEvidenceRequired(), policy.effectiveFrom(),
          policy.effectiveTo(), policy.active());
    }
  }

  public record TicketResponse(
      UUID id,
      String ticketNumber,
      UUID companyId,
      UUID customerId,
      UUID productId,
      UUID skuId,
      String serialCode,
      String status,
      Boolean warrantyEligible,
      String warrantyReason,
      String diagnosis,
      String resolution,
      long version) {
    static TicketResponse from(final ServiceTicket ticket) {
      return new TicketResponse(
          ticket.id(), ticket.ticketNumber(), ticket.companyId(), ticket.customerId(),
          ticket.productId(), ticket.skuId(), ticket.serialCode(), ticket.status().name(),
          ticket.warrantyDecision() == null ? null : ticket.warrantyDecision().eligible(),
          ticket.warrantyDecision() == null ? "" : ticket.warrantyDecision().reason(),
          ticket.diagnosis() == null ? "" : ticket.diagnosis().findings(),
          ticket.resolution() == null ? "" : ticket.resolution().outcome(),
          ticket.version());
    }
  }

  private ServiceWarrantyDtos() {}
}
