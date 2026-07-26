package com.newland.erp.crm.api;

import com.newland.erp.crm.domain.Activity;
import com.newland.erp.crm.domain.Lead;
import com.newland.erp.crm.domain.Opportunity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class CrmDtos {
  public record CreateLeadRequest(
      @NotBlank String idempotencyKey,
      @NotNull UUID companyId,
      UUID branchId,
      @NotNull UUID ownerId,
      @NotBlank String leadNumber,
      @NotBlank String organizationName,
      @NotBlank String contactName,
      @Email String email,
      String phone,
      @NotBlank String source) {
    Lead domain(final String actor) {
      final Instant now = Instant.now();
      return new Lead(
          UUID.randomUUID(), idempotencyKey, companyId, branchId, ownerId, leadNumber,
          organizationName, contactName, email, phone, source, Lead.Status.NEW, "", 0,
          now, now, actor);
    }
  }

  public record CreateOpportunityRequest(
      @NotBlank String idempotencyKey,
      @NotNull UUID companyId,
      UUID branchId,
      @NotNull UUID ownerId,
      UUID leadId,
      UUID customerId,
      @NotBlank String opportunityNumber,
      @NotBlank String name,
      @NotNull @DecimalMin("0") BigDecimal estimatedValue,
      @NotBlank String currencyCode,
      @Min(0) @Max(100) int probabilityPercent,
      @NotNull LocalDate expectedCloseDate) {
    Opportunity domain(final String actor) {
      final Instant now = Instant.now();
      return new Opportunity(
          UUID.randomUUID(), idempotencyKey, companyId, branchId, ownerId, leadId, customerId,
          opportunityNumber, name, Opportunity.Stage.QUALIFICATION, estimatedValue,
          currencyCode, probabilityPercent, expectedCloseDate, "", 0, now, now, actor);
    }
  }

  public record StageRequest(@NotBlank String stage, String reason) {}

  public record ReasonRequest(@NotBlank String reason) {}

  public record CreateActivityRequest(
      @NotBlank String idempotencyKey,
      @NotNull UUID companyId,
      UUID customerId,
      UUID leadId,
      UUID opportunityId,
      @NotBlank String type,
      @NotBlank String subject,
      String details,
      @NotNull Instant occurredAt,
      Instant followUpAt) {
    Activity domain(final String actor) {
      return new Activity(
          UUID.randomUUID(), idempotencyKey, companyId, customerId, leadId, opportunityId,
          Activity.Type.from(type), subject, details, occurredAt, followUpAt, actor);
    }
  }

  public record LeadResponse(
      UUID id,
      String leadNumber,
      UUID companyId,
      UUID branchId,
      UUID ownerId,
      String organizationName,
      String contactName,
      String status,
      long version) {
    static LeadResponse from(final Lead lead) {
      return new LeadResponse(
          lead.id(), lead.leadNumber(), lead.companyId(), lead.branchId(), lead.ownerId(),
          lead.organizationName(), lead.contactName(), lead.status().name(), lead.version());
    }
  }

  public record OpportunityResponse(
      UUID id,
      String opportunityNumber,
      UUID companyId,
      UUID leadId,
      UUID customerId,
      String name,
      String stage,
      BigDecimal estimatedValue,
      String currencyCode,
      int probabilityPercent,
      LocalDate expectedCloseDate,
      long version) {
    static OpportunityResponse from(final Opportunity opportunity) {
      return new OpportunityResponse(
          opportunity.id(), opportunity.opportunityNumber(), opportunity.companyId(),
          opportunity.leadId(), opportunity.customerId(), opportunity.name(),
          opportunity.stage().name(), opportunity.estimatedValue(),
          opportunity.currencyCode(), opportunity.probabilityPercent(),
          opportunity.expectedCloseDate(), opportunity.version());
    }
  }

  public record ActivityResponse(
      UUID id,
      UUID customerId,
      UUID leadId,
      UUID opportunityId,
      String type,
      String subject,
      String details,
      Instant occurredAt,
      Instant followUpAt,
      String actor) {
    static ActivityResponse from(final Activity activity) {
      return new ActivityResponse(
          activity.id(), activity.customerId(), activity.leadId(), activity.opportunityId(),
          activity.type().name(), activity.subject(), activity.details(), activity.occurredAt(),
          activity.followUpAt(), activity.actor());
    }
  }

  private CrmDtos() {}
}
