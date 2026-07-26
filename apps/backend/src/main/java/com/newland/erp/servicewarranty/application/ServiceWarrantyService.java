package com.newland.erp.servicewarranty.application;

import com.newland.erp.enterprise.application.integration.EnterpriseReferencePort;
import com.newland.erp.inventory.application.integration.InventorySerialReferencePort;
import com.newland.erp.platform.application.integration.PlatformAuditOutboxPort;
import com.newland.erp.productcatalog.application.integration.ProductWarrantyReferencePort;
import com.newland.erp.sales.application.integration.SalesCustomerReferencePort;
import com.newland.erp.sales.application.integration.SalesWarrantyEvidencePort;
import com.newland.erp.servicewarranty.domain.ServiceTicket;
import com.newland.erp.servicewarranty.domain.WarrantyPolicy;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceWarrantyService {
  private final ServiceWarrantyRepository repository;
  private final ServiceWarrantySecurityPort security;
  private final EnterpriseReferencePort enterprise;
  private final SalesCustomerReferencePort customers;
  private final ProductWarrantyReferencePort products;
  private final InventorySerialReferencePort serials;
  private final SalesWarrantyEvidencePort salesEvidence;
  private final PlatformAuditOutboxPort platform;
  private final Clock clock;

  public ServiceWarrantyService(
      final ServiceWarrantyRepository serviceRepository,
      final ServiceWarrantySecurityPort securityPort,
      final EnterpriseReferencePort enterprisePort,
      final SalesCustomerReferencePort customerPort,
      final ProductWarrantyReferencePort productPort,
      final InventorySerialReferencePort serialPort,
      final SalesWarrantyEvidencePort evidencePort,
      final PlatformAuditOutboxPort platformPort,
      final Clock systemClock) {
    repository = serviceRepository;
    security = securityPort;
    enterprise = enterprisePort;
    customers = customerPort;
    products = productPort;
    serials = serialPort;
    salesEvidence = evidencePort;
    platform = platformPort;
    clock = systemClock;
  }

  @Transactional
  public WarrantyPolicy createPolicy(final WarrantyPolicy policy, final String actor) {
    security.require(actor, "service.warranty-policy.manage", policy.companyId());
    if (!enterprise.isActiveCompany(policy.companyId())) {
      throw new IllegalArgumentException("Warranty policy company is inactive or invalid.");
    }
    if (policy.productId() != null) {
      products.requireProduct(policy.productId());
    }
    if (repository.hasOverlappingPolicy(policy)) {
      throw new IllegalArgumentException("Active warranty policy effective periods overlap.");
    }
    final WarrantyPolicy saved;
    try {
      saved = repository.insertPolicy(policy);
    } catch (DataIntegrityViolationException exception) {
      throw new IllegalArgumentException(
          "Active warranty policy effective periods overlap.", exception);
    }
    audit(actor, "SERVICE_WARRANTY_POLICY_CREATED", "WarrantyPolicy", saved.id());
    return saved;
  }

  @Transactional
  public ServiceTicket createTicket(final ServiceTicket candidate) {
    security.require(candidate.actor(), "service.ticket.manage", candidate.companyId());
    requireScope(candidate.companyId(), candidate.branchId());
    customers.requireCustomer(candidate.customerId(), candidate.companyId());
    products.requireWarrantyProduct(candidate.productId(), candidate.skuId());
    if (!candidate.serialCode().isBlank()) {
      serials.requireSerial(candidate.skuId(), candidate.serialCode());
    }
    final var existing =
        repository.findTicketByIdempotencyKey(candidate.idempotencyKey());
    if (existing.isPresent()) {
      return requireSameTicket(candidate, existing.get());
    }
    if (!repository.insertTicketIfAbsent(candidate)) {
      return requireSameTicket(
          candidate,
          repository
              .findTicketByIdempotencyKey(candidate.idempotencyKey())
              .orElseThrow(() -> new IllegalStateException("Service ticket creation conflicted.")));
    }
    platform.publishEvent(
        "servicewarranty",
        "ServiceTicketCreated",
        candidate.id(),
        Map.of("customerId", candidate.customerId().toString()));
    audit(candidate.actor(), "SERVICE_TICKET_CREATED", "ServiceTicket", candidate.id());
    return candidate;
  }

  @Transactional
  public ServiceTicket validateWarranty(final UUID ticketId, final String actor) {
    ServiceTicket ticket = ticket(ticketId);
    security.require(actor, "service.warranty.validate", ticket.companyId());
    ticket = repository.updateTicket(ticket.beginValidation(Instant.now(clock)));
    final LocalDate decisionDate = LocalDate.now(clock);
    final WarrantyPolicy policy =
        repository
            .resolvePolicy(ticket.companyId(), ticket.productId(), decisionDate)
            .orElseThrow(() -> new IllegalStateException("No active warranty policy applies."));
    final ServiceTicket.WarrantyDecision decision = evaluate(ticket, policy, actor, decisionDate);
    final ServiceTicket validated =
        repository.updateTicket(ticket.recordWarranty(decision, Instant.now(clock)));
    platform.publishEvent(
        "servicewarranty",
        "WarrantyValidated",
        validated.id(),
        Map.of("eligible", Boolean.toString(decision.eligible())));
    audit(actor, "SERVICE_WARRANTY_VALIDATED", "ServiceTicket", validated.id());
    return validated;
  }

  @Transactional
  public ServiceTicket diagnose(
      final UUID ticketId,
      final String findings,
      final String recommendation,
      final String actor) {
    final ServiceTicket ticket = ticket(ticketId);
    security.require(actor, "service.ticket.diagnose", ticket.companyId());
    final Instant now = Instant.now(clock);
    final var diagnosis =
        new ServiceTicket.Diagnosis(UUID.randomUUID(), findings, recommendation, now);
    final ServiceTicket diagnosed = repository.updateTicket(ticket.diagnose(diagnosis, now));
    audit(actor, "SERVICE_TICKET_DIAGNOSED", "ServiceTicket", ticketId);
    return diagnosed;
  }

  @Transactional
  public ServiceTicket approveResolution(
      final UUID ticketId,
      final ServiceTicket.Resolution.Type type,
      final String notes,
      final String actor) {
    final ServiceTicket ticket = ticket(ticketId);
    security.require(actor, "service.ticket.approve-resolution", ticket.companyId());
    if (type == ServiceTicket.Resolution.Type.CANCELLED) {
      throw new IllegalArgumentException("Approved resolution must be repair or replacement.");
    }
    final ServiceTicket approved =
        repository.updateTicket(ticket.approveResolution(type, notes, Instant.now(clock)));
    audit(actor, "SERVICE_RESOLUTION_APPROVED", "ServiceTicket", ticketId);
    return approved;
  }

  @Transactional
  public ServiceTicket close(final UUID ticketId, final String outcome, final String actor) {
    final ServiceTicket ticket = ticket(ticketId);
    security.require(actor, "service.ticket.close", ticket.companyId());
    final ServiceTicket closed =
        repository.updateTicket(ticket.close(outcome, Instant.now(clock)));
    platform.publishEvent("servicewarranty", "ServiceTicketClosed", closed.id(), Map.of());
    audit(actor, "SERVICE_TICKET_CLOSED", "ServiceTicket", ticketId);
    return closed;
  }

  @Transactional(readOnly = true)
  public ServiceTicket ticket(final UUID ticketId) {
    return repository
        .findTicket(ticketId)
        .orElseThrow(() -> new IllegalArgumentException("Service ticket not found."));
  }

  @Transactional(readOnly = true)
  public ServiceTicket ticket(final UUID ticketId, final UUID companyId) {
    return repository
        .findTicket(ticketId, companyId)
        .orElseThrow(() -> new IllegalArgumentException("Service ticket not found."));
  }

  private ServiceTicket.WarrantyDecision evaluate(
      final ServiceTicket ticket,
      final WarrantyPolicy policy,
      final String actor,
      final LocalDate decisionDate) {
    if (policy.serialRequired()) {
      if (ticket.serialCode().isBlank()) {
        return decision(policy, false, "Required serial number is missing.", null, actor);
      }
      serials.requireSerial(ticket.skuId(), ticket.serialCode());
    }
    LocalDate purchaseDate = ticket.purchaseDate();
    if (policy.salesEvidenceRequired()) {
      if (ticket.salesOrderId() == null) {
        return decision(policy, false, "Required sales evidence is missing.", null, actor);
      }
      purchaseDate =
          salesEvidence
              .requireDeliveredEvidence(
                  ticket.salesOrderId(), ticket.companyId(), ticket.customerId(),
                  ticket.productId(), ticket.skuId())
              .soldOn();
    }
    if (purchaseDate == null) {
      return decision(policy, false, "Warranty start date is unavailable.", null, actor);
    }
    final LocalDate coverageEnds = purchaseDate.plusDays(policy.durationDays());
    final boolean eligible = !decisionDate.isAfter(coverageEnds);
    return decision(
        policy,
        eligible,
        eligible ? "Warranty coverage is valid." : "Warranty coverage has expired.",
        coverageEnds,
        actor);
  }

  private ServiceTicket.WarrantyDecision decision(
      final WarrantyPolicy policy,
      final boolean eligible,
      final String reason,
      final LocalDate coverageEnds,
      final String actor) {
    return new ServiceTicket.WarrantyDecision(
        UUID.randomUUID(), policy.id(), eligible, reason, coverageEnds, Instant.now(clock), actor);
  }

  private void requireScope(final UUID companyId, final UUID branchId) {
    if (!enterprise.isActiveCompany(companyId)
        || (branchId != null && !enterprise.isActiveBranch(companyId, branchId))) {
      throw new IllegalArgumentException("Service company or branch is inactive or invalid.");
    }
  }

  private void audit(
      final String actor, final String action, final String targetType, final UUID targetId) {
    platform.recordAudit(actor, action, targetType, targetId, Map.of());
  }

  private static ServiceTicket requireSameTicket(
      final ServiceTicket candidate, final ServiceTicket existing) {
    if (!candidate.companyId().equals(existing.companyId())
        || !candidate.ticketNumber().equals(existing.ticketNumber())
        || !candidate.customerId().equals(existing.customerId())
        || !candidate.productId().equals(existing.productId())
        || !candidate.skuId().equals(existing.skuId())
        || !candidate.serialCode().equals(existing.serialCode())
        || !candidate.issueSummary().equals(existing.issueSummary())) {
      throw new IllegalArgumentException(
          "Idempotency key conflicts with existing service ticket.");
    }
    return existing;
  }

}
