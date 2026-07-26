package com.newland.erp.crm.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.newland.erp.crm.domain.Activity;
import com.newland.erp.crm.domain.Lead;
import com.newland.erp.crm.domain.Opportunity;
import com.newland.erp.enterprise.application.integration.EnterpriseReferencePort;
import com.newland.erp.platform.application.integration.PlatformAuditOutboxPort;
import com.newland.erp.sales.application.integration.SalesCustomerReferencePort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class CrmServiceTest {
  private static final Instant NOW = Instant.parse("2026-07-26T00:00:00Z");
  private static final UUID COMPANY_ID = UUID.randomUUID();
  private static final UUID BRANCH_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private CrmRepository repository;
  private CrmSecurityPort security;
  private EnterpriseReferencePort enterprise;
  private SalesCustomerReferencePort customers;
  private PlatformAuditOutboxPort platform;
  private CrmService service;

  @BeforeEach
  void setUp() {
    repository = mock(CrmRepository.class);
    security = mock(CrmSecurityPort.class);
    enterprise = mock(EnterpriseReferencePort.class);
    customers = mock(SalesCustomerReferencePort.class);
    platform = mock(PlatformAuditOutboxPort.class);
    when(enterprise.isActiveCompany(COMPANY_ID)).thenReturn(true);
    when(enterprise.isActiveBranch(COMPANY_ID, BRANCH_ID)).thenReturn(true);
    service =
        new CrmService(
            repository, security, enterprise, customers, platform,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void createsLeadWithScopeAuthorizationAuditAndAtomicIdempotency() {
    final Lead lead = lead();
    when(repository.findLeadByIdempotencyKey(lead.idempotencyKey()))
        .thenReturn(Optional.empty());
    when(repository.insertLeadIfAbsent(lead)).thenReturn(true);

    assertThat(service.createLead(lead)).isEqualTo(lead);

    verify(security).require(USER_ID.toString(), "crm.lead.manage", COMPANY_ID);
    verify(platform)
        .recordAudit(
            USER_ID.toString(), "CRM_LEAD_CREATED", "Lead", lead.id(), java.util.Map.of());
  }

  @Test
  void duplicateLeadReturnsExistingWithoutDuplicateAudit() {
    final Lead lead = lead();
    when(repository.findLeadByIdempotencyKey(lead.idempotencyKey()))
        .thenReturn(Optional.of(lead));

    assertThat(service.createLead(lead)).isEqualTo(lead);
    verify(repository, never()).insertLeadIfAbsent(any());
    verify(platform, never())
        .recordAudit(anyString(), anyString(), anyString(), any(), any());
  }

  @Test
  void rejectsOpportunityForUnqualifiedLead() {
    final Lead lead = lead();
    final Opportunity opportunity = opportunity(lead.id(), null);
    when(repository.findLead(lead.id())).thenReturn(Optional.of(lead));

    assertThatThrownBy(() -> service.createOpportunity(opportunity))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("qualified lead");
  }

  @Test
  void validatesCustomerAndProducesCompanyScopedTimeline() {
    final UUID customerId = UUID.randomUUID();
    final Activity activity =
        new Activity(
            UUID.randomUUID(), "activity-key", COMPANY_ID, customerId, null, null,
            Activity.Type.CALL, "Call", "", NOW, null, USER_ID.toString());
    when(customers.requireCustomer(customerId, COMPANY_ID))
        .thenReturn(new SalesCustomerReferencePort.CustomerReference(
            customerId, "CUS-1", "Customer"));
    when(repository.listCustomerActivities(COMPANY_ID, customerId))
        .thenReturn(List.of(activity));

    assertThat(service.customerTimeline(COMPANY_ID, customerId, USER_ID.toString()))
        .containsExactly(activity);
    verify(security).require(USER_ID.toString(), "crm.timeline.read", COMPANY_ID);
  }

  private static Lead lead() {
    return new Lead(
        UUID.randomUUID(), "lead-key", COMPANY_ID, BRANCH_ID, USER_ID, "LEAD-1",
        "Organization", "Contact", "contact@example.com", "", "WEB", Lead.Status.NEW,
        "", 0, NOW, NOW, USER_ID.toString());
  }

  private static Opportunity opportunity(final UUID leadId, final UUID customerId) {
    return new Opportunity(
        UUID.randomUUID(), "opportunity-key", COMPANY_ID, BRANCH_ID, USER_ID, leadId,
        customerId, "OPP-1", "Opportunity", Opportunity.Stage.QUALIFICATION,
        new BigDecimal("1000"), "USD", 20, LocalDate.of(2026, 9, 1), "", 0,
        NOW, NOW, USER_ID.toString());
  }
}
