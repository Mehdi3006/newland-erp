package com.newland.erp.crm.application;

import com.newland.erp.crm.domain.Activity;
import com.newland.erp.crm.domain.Lead;
import com.newland.erp.crm.domain.Opportunity;
import com.newland.erp.enterprise.application.integration.EnterpriseReferencePort;
import com.newland.erp.platform.application.integration.PlatformAuditOutboxPort;
import com.newland.erp.sales.application.integration.SalesCustomerReferencePort;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public final class CrmService {
  private final CrmRepository repository;
  private final CrmSecurityPort security;
  private final EnterpriseReferencePort enterprise;
  private final SalesCustomerReferencePort customers;
  private final PlatformAuditOutboxPort platform;
  private final Clock clock;

  public CrmService(
      final CrmRepository crmRepository,
      final CrmSecurityPort securityPort,
      final EnterpriseReferencePort enterpriseReferencePort,
      final SalesCustomerReferencePort customerReferencePort,
      final PlatformAuditOutboxPort platformPort,
      final Clock systemClock) {
    repository = crmRepository;
    security = securityPort;
    enterprise = enterpriseReferencePort;
    customers = customerReferencePort;
    platform = platformPort;
    clock = systemClock;
  }

  @Transactional
  public Lead createLead(final Lead candidate) {
    security.require(candidate.actor(), "crm.lead.manage", candidate.companyId());
    requireScope(candidate.companyId(), candidate.branchId());
    final var existing = repository.findLeadByIdempotencyKey(candidate.idempotencyKey());
    if (existing.isPresent()) {
      return requireSameLead(candidate, existing.get());
    }
    if (!repository.insertLeadIfAbsent(candidate)) {
      return requireSameLead(
          candidate,
          repository
              .findLeadByIdempotencyKey(candidate.idempotencyKey())
              .orElseThrow(() -> new IllegalStateException("Lead creation conflicted.")));
    }
    audit(candidate.actor(), "CRM_LEAD_CREATED", "Lead", candidate.id());
    return candidate;
  }

  @Transactional
  public Lead qualifyLead(final UUID leadId, final String actor) {
    final Lead lead = lead(leadId);
    security.require(actor, "crm.lead.qualify", lead.companyId());
    final Lead qualified = repository.updateLead(lead.qualify(Instant.now(clock)));
    platform.publishEvent(
        "crm", "LeadQualified", qualified.id(), Map.of("companyId", qualified.companyId().toString()));
    audit(actor, "CRM_LEAD_QUALIFIED", "Lead", leadId);
    return qualified;
  }

  @Transactional
  public Lead disqualifyLead(final UUID leadId, final String reason, final String actor) {
    final Lead lead = lead(leadId);
    security.require(actor, "crm.lead.qualify", lead.companyId());
    final Lead disqualified =
        repository.updateLead(lead.disqualify(reason, Instant.now(clock)));
    audit(actor, "CRM_LEAD_DISQUALIFIED", "Lead", leadId);
    return disqualified;
  }

  @Transactional
  public Opportunity createOpportunity(final Opportunity candidate) {
    security.require(candidate.actor(), "crm.opportunity.manage", candidate.companyId());
    requireScope(candidate.companyId(), candidate.branchId());
    validateOpportunityReferences(candidate);
    final var existing =
        repository.findOpportunityByIdempotencyKey(candidate.idempotencyKey());
    if (existing.isPresent()) {
      return requireSameOpportunity(candidate, existing.get());
    }
    if (!repository.insertOpportunityIfAbsent(candidate)) {
      return requireSameOpportunity(
          candidate,
          repository
              .findOpportunityByIdempotencyKey(candidate.idempotencyKey())
              .orElseThrow(() -> new IllegalStateException("Opportunity creation conflicted.")));
    }
    if (candidate.leadId() != null) {
      final Lead source = lead(candidate.leadId());
      if (source.status() == Lead.Status.QUALIFIED) {
        repository.updateLead(source.convert("Converted to opportunity", Instant.now(clock)));
      }
    }
    audit(candidate.actor(), "CRM_OPPORTUNITY_CREATED", "Opportunity", candidate.id());
    return candidate;
  }

  @Transactional
  public Opportunity advanceOpportunity(
      final UUID opportunityId,
      final Opportunity.Stage stage,
      final String reason,
      final String actor) {
    final Opportunity opportunity = opportunity(opportunityId);
    security.require(actor, "crm.opportunity.manage", opportunity.companyId());
    final Opportunity advanced =
        repository.updateOpportunity(
            opportunity.advance(stage, reason, Instant.now(clock)));
    if (stage == Opportunity.Stage.WON || stage == Opportunity.Stage.LOST) {
      platform.publishEvent(
          "crm",
          stage == Opportunity.Stage.WON ? "OpportunityWon" : "OpportunityLost",
          advanced.id(),
          Map.of("companyId", advanced.companyId().toString()));
    }
    audit(actor, "CRM_OPPORTUNITY_STAGE_CHANGED", "Opportunity", opportunityId);
    return advanced;
  }

  @Transactional
  public Activity recordActivity(final Activity candidate) {
    security.require(candidate.actor(), "crm.activity.create", candidate.companyId());
    validateActivityReferences(candidate);
    final var existing =
        repository.findActivityByIdempotencyKey(candidate.idempotencyKey());
    if (existing.isPresent()) {
      return requireSameActivity(candidate, existing.get());
    }
    if (!repository.insertActivityIfAbsent(candidate)) {
      return requireSameActivity(
          candidate,
          repository
              .findActivityByIdempotencyKey(candidate.idempotencyKey())
              .orElseThrow(() -> new IllegalStateException("Activity creation conflicted.")));
    }
    audit(candidate.actor(), "CRM_ACTIVITY_RECORDED", "Activity", candidate.id());
    return candidate;
  }

  @Transactional(readOnly = true)
  public List<Activity> customerTimeline(
      final UUID companyId, final UUID customerId, final String actor) {
    security.require(actor, "crm.timeline.read", companyId);
    customers.requireCustomer(customerId, companyId);
    return repository.listCustomerActivities(companyId, customerId);
  }

  private void validateOpportunityReferences(final Opportunity opportunity) {
    if (opportunity.leadId() != null) {
      final Lead lead = lead(opportunity.leadId());
      if (!lead.companyId().equals(opportunity.companyId())
          || lead.status() != Lead.Status.QUALIFIED) {
        throw new IllegalArgumentException(
            "Opportunity requires a qualified lead in the same company.");
      }
    }
    if (opportunity.customerId() != null) {
      customers.requireCustomer(opportunity.customerId(), opportunity.companyId());
    }
  }

  private void validateActivityReferences(final Activity activity) {
    if (activity.customerId() != null) {
      customers.requireCustomer(activity.customerId(), activity.companyId());
    }
    if (activity.leadId() != null
        && !lead(activity.leadId()).companyId().equals(activity.companyId())) {
      throw new IllegalArgumentException("Activity lead is outside company scope.");
    }
    if (activity.opportunityId() != null
        && !opportunity(activity.opportunityId()).companyId().equals(activity.companyId())) {
      throw new IllegalArgumentException("Activity opportunity is outside company scope.");
    }
  }

  private void requireScope(final UUID companyId, final UUID branchId) {
    if (!enterprise.isActiveCompany(companyId)
        || (branchId != null && !enterprise.isActiveBranch(companyId, branchId))) {
      throw new IllegalArgumentException("CRM company or branch is inactive or invalid.");
    }
  }

  private Lead lead(final UUID leadId) {
    return repository
        .findLead(leadId)
        .orElseThrow(() -> new IllegalArgumentException("CRM lead not found."));
  }

  private Opportunity opportunity(final UUID opportunityId) {
    return repository
        .findOpportunity(opportunityId)
        .orElseThrow(() -> new IllegalArgumentException("CRM opportunity not found."));
  }

  private void audit(
      final String actor, final String action, final String targetType, final UUID targetId) {
    platform.recordAudit(actor, action, targetType, targetId, Map.of());
  }

  private static Lead requireSameLead(final Lead candidate, final Lead existing) {
    if (!candidate.companyId().equals(existing.companyId())
        || !candidate.leadNumber().equals(existing.leadNumber())
        || !candidate.organizationName().equals(existing.organizationName())
        || !candidate.contactName().equals(existing.contactName())
        || !candidate.email().equals(existing.email())
        || !candidate.phone().equals(existing.phone())) {
      throw new IllegalArgumentException("Idempotency key conflicts with existing lead.");
    }
    return existing;
  }

  private static Opportunity requireSameOpportunity(
      final Opportunity candidate, final Opportunity existing) {
    if (!candidate.companyId().equals(existing.companyId())
        || !candidate.opportunityNumber().equals(existing.opportunityNumber())
        || !java.util.Objects.equals(candidate.leadId(), existing.leadId())
        || !java.util.Objects.equals(candidate.customerId(), existing.customerId())
        || candidate.estimatedValue().compareTo(existing.estimatedValue()) != 0) {
      throw new IllegalArgumentException(
          "Idempotency key conflicts with existing opportunity.");
    }
    return existing;
  }

  private static Activity requireSameActivity(
      final Activity candidate, final Activity existing) {
    if (!candidate.companyId().equals(existing.companyId())
        || !java.util.Objects.equals(candidate.customerId(), existing.customerId())
        || !java.util.Objects.equals(candidate.leadId(), existing.leadId())
        || !java.util.Objects.equals(candidate.opportunityId(), existing.opportunityId())
        || candidate.type() != existing.type()
        || !candidate.subject().equals(existing.subject())
        || !candidate.occurredAt().equals(existing.occurredAt())) {
      throw new IllegalArgumentException("Idempotency key conflicts with existing activity.");
    }
    return existing;
  }
}
