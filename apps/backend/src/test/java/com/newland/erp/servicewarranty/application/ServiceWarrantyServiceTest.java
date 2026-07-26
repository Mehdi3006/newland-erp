package com.newland.erp.servicewarranty.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class ServiceWarrantyServiceTest {
  private static final Instant NOW = Instant.parse("2026-07-26T00:00:00Z");
  private static final UUID COMPANY_ID = UUID.randomUUID();
  private static final UUID CUSTOMER_ID = UUID.randomUUID();
  private static final UUID PRODUCT_ID = UUID.randomUUID();
  private static final UUID SKU_ID = UUID.randomUUID();
  private static final UUID SALES_ORDER_ID = UUID.randomUUID();
  private static final String ACTOR = UUID.randomUUID().toString();
  private ServiceWarrantyRepository repository;
  private ServiceWarrantySecurityPort security;
  private InventorySerialReferencePort serials;
  private SalesWarrantyEvidencePort evidence;
  private PlatformAuditOutboxPort platform;
  private ServiceWarrantyService service;

  @BeforeEach
  void setUp() {
    repository = mock(ServiceWarrantyRepository.class);
    security = mock(ServiceWarrantySecurityPort.class);
    final EnterpriseReferencePort enterprise = mock(EnterpriseReferencePort.class);
    final SalesCustomerReferencePort customers = mock(SalesCustomerReferencePort.class);
    final ProductWarrantyReferencePort products = mock(ProductWarrantyReferencePort.class);
    serials = mock(InventorySerialReferencePort.class);
    evidence = mock(SalesWarrantyEvidencePort.class);
    platform = mock(PlatformAuditOutboxPort.class);
    when(enterprise.isActiveCompany(COMPANY_ID)).thenReturn(true);
    service =
        new ServiceWarrantyService(
            repository, security, enterprise, customers, products, serials, evidence, platform,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void validatesWarrantyUsingAuthoritativeSerialAndDeliveredSalesEvidence() {
    final ServiceTicket ticket = ticket();
    final ServiceTicket validating = ticket.beginValidation(NOW);
    final WarrantyPolicy policy =
        new WarrantyPolicy(
            UUID.randomUUID(), COMPANY_ID, PRODUCT_ID, 365, true, true,
            LocalDate.of(2026, 1, 1), null, true);
    when(repository.findTicket(ticket.id())).thenReturn(Optional.of(ticket));
    when(repository.updateTicket(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(repository.resolvePolicy(COMPANY_ID, PRODUCT_ID, LocalDate.of(2026, 7, 26)))
        .thenReturn(Optional.of(policy));
    when(evidence.requireDeliveredEvidence(
            SALES_ORDER_ID, COMPANY_ID, CUSTOMER_ID, PRODUCT_ID, SKU_ID))
        .thenReturn(
            new SalesWarrantyEvidencePort.SalesEvidence(
                SALES_ORDER_ID, LocalDate.of(2026, 1, 1)));

    final ServiceTicket result = service.validateWarranty(ticket.id(), ACTOR);

    assertThat(result.status()).isEqualTo(ServiceTicket.Status.WARRANTY_VALID);
    assertThat(result.warrantyDecision().coverageEndsOn())
        .isEqualTo(LocalDate.of(2027, 1, 1));
    verify(serials).requireSerial(SKU_ID, "SERIAL-1");
    verify(security).require(ACTOR, "service.warranty.validate", COMPANY_ID);
    assertThat(validating.status()).isEqualTo(ServiceTicket.Status.VALIDATING);
  }

  @Test
  void recordsExpiredWarrantyAsAuditableRejection() {
    final ServiceTicket ticket = ticket();
    final WarrantyPolicy policy =
        new WarrantyPolicy(
            UUID.randomUUID(), COMPANY_ID, PRODUCT_ID, 30, false, false,
            LocalDate.of(2025, 1, 1), null, true);
    when(repository.findTicket(ticket.id())).thenReturn(Optional.of(ticket));
    when(repository.updateTicket(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(repository.resolvePolicy(COMPANY_ID, PRODUCT_ID, LocalDate.of(2026, 7, 26)))
        .thenReturn(Optional.of(policy));

    final ServiceTicket result = service.validateWarranty(ticket.id(), ACTOR);

    assertThat(result.status()).isEqualTo(ServiceTicket.Status.WARRANTY_REJECTED);
    assertThat(result.warrantyDecision().reason()).contains("expired");
  }

  private static ServiceTicket ticket() {
    return new ServiceTicket(
        UUID.randomUUID(), "ticket-key", "SRV-1", COMPANY_ID, null, CUSTOMER_ID, PRODUCT_ID,
        SKU_ID, "SERIAL-1", SALES_ORDER_ID, LocalDate.of(2026, 1, 1), "Issue",
        ServiceTicket.Status.OPEN, null, null, null, 0, NOW, NOW, ACTOR);
  }
}
