package com.newland.erp.servicewarranty.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newland.erp.enterprise.application.integration.EnterpriseReferencePort;
import com.newland.erp.inventory.application.integration.InventorySerialReferencePort;
import com.newland.erp.productcatalog.application.integration.ProductWarrantyReferencePort;
import com.newland.erp.sales.application.integration.SalesCustomerReferencePort;
import com.newland.erp.sales.application.integration.SalesWarrantyEvidencePort;
import com.newland.erp.servicewarranty.domain.ServiceTicket;
import com.newland.erp.servicewarranty.application.ServiceWarrantySecurityPort;
import com.newland.erp.servicewarranty.application.ServiceWarrantyService;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = ServiceWarrantyTransactionIntegrationTest.TestApplication.class)
@Testcontainers(disabledWithoutDocker = true)
final class ServiceWarrantyTransactionIntegrationTest {
  private static final UUID COMPANY_ID =
      UUID.fromString("44000000-0000-4000-8000-000000000003");
  private static final Instant NOW = Instant.parse("2026-07-26T00:00:00Z");

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17-alpine");

  @Autowired private DSLContext dsl;
  @Autowired private TransactionTemplate transactions;
  @Autowired private ServiceWarrantyService service;
  @MockitoBean private ServiceWarrantySecurityPort security;
  @MockitoBean private EnterpriseReferencePort enterprise;
  @MockitoBean private SalesCustomerReferencePort customers;
  @MockitoBean private ProductWarrantyReferencePort products;
  @MockitoBean private InventorySerialReferencePort serials;
  @MockitoBean private SalesWarrantyEvidencePort salesEvidence;

  @DynamicPropertySource
  static void properties(final DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add(
        "newland.security.jwt.secret",
        () -> "test-only-7aa711f462dd408fb66799e89a10e7d8");
    registry.add("newland.platform.outbox.dispatch-delay", () -> "600000");
  }

  @BeforeEach
  void setUp() {
    dsl.deleteFrom(DSL.table("platform_outbox")).execute();
    dsl.deleteFrom(DSL.table("platform_audit_log")).execute();
    dsl.deleteFrom(DSL.table("service_ticket")).execute();
    dsl.deleteFrom(DSL.table("company")).execute();
    dsl.deleteFrom(DSL.table("legal_entity")).execute();
    dsl.deleteFrom(DSL.table("enterprise")).execute();
    seedEnterprise();
    when(enterprise.isActiveCompany(COMPANY_ID)).thenReturn(true);
  }

  @Test
  void commitsTicketAuditAndOutboxAtomically() {
    transactions.executeWithoutResult(status -> service.createTicket(ticket("commit-key")));

    assertThat(count("service_ticket")).isEqualTo(1);
    assertThat(count("platform_audit_log")).isEqualTo(1);
    assertThat(count("platform_outbox")).isEqualTo(1);
  }

  @Test
  void rollsBackTicketAuditAndOutboxTogether() {
    assertThatThrownBy(
            () ->
                transactions.executeWithoutResult(
                    status -> {
                      service.createTicket(ticket("rollback-key"));
                      throw new IllegalStateException("force rollback");
                    }))
        .isInstanceOf(IllegalStateException.class);

    assertThat(count("service_ticket")).isZero();
    assertThat(count("platform_audit_log")).isZero();
    assertThat(count("platform_outbox")).isZero();
  }

  private ServiceTicket ticket(final String idempotencyKey) {
    return new ServiceTicket(
        UUID.randomUUID(),
        idempotencyKey,
        "SRV-" + UUID.randomUUID(),
        COMPANY_ID,
        null,
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "",
        null,
        null,
        "Device failure",
        ServiceTicket.Status.OPEN,
        null,
        null,
        null,
        0,
        NOW,
        NOW,
        UUID.randomUUID().toString());
  }

  private int count(final String table) {
    return dsl.fetchCount(DSL.table(DSL.name(table)));
  }

  private void seedEnterprise() {
    dsl.execute(
        """
        INSERT INTO enterprise
          (id, code, name, status, created_at, created_by, updated_at, updated_by)
        VALUES ('44000000-0000-4000-8000-000000000001', 'ENT', 'Enterprise', 'ACTIVE',
                now(), 'test', now(), 'test');
        INSERT INTO legal_entity
          (id, enterprise_id, code, name, country_code, base_currency, status,
           created_at, created_by, updated_at, updated_by)
        VALUES ('44000000-0000-4000-8000-000000000002',
                '44000000-0000-4000-8000-000000000001', 'LE', 'Legal Entity', 'CN', 'USD',
                'ACTIVE', now(), 'test', now(), 'test');
        INSERT INTO company
          (id, enterprise_id, legal_entity_id, code, name, country_code, base_currency,
           time_zone_id, status, created_at, created_by, updated_at, updated_by)
        VALUES ('44000000-0000-4000-8000-000000000003',
                '44000000-0000-4000-8000-000000000001',
                '44000000-0000-4000-8000-000000000002', 'COMP', 'Company', 'CN', 'USD',
                'Asia/Shanghai', 'ACTIVE', now(), 'test', now(), 'test');
        """);
  }

  @SpringBootConfiguration
  @EnableAutoConfiguration
  @ComponentScan(
      basePackages = {
        "com.newland.erp.platform.application",
        "com.newland.erp.platform.infrastructure",
        "com.newland.erp.servicewarranty.application",
        "com.newland.erp.servicewarranty.infrastructure"
      })
  static class TestApplication {
    @Bean
    Clock testClock() {
      return java.time.Clock.fixed(NOW, java.time.ZoneOffset.UTC);
    }

    @Bean
    ObjectMapper objectMapper() {
      return new ObjectMapper();
    }
  }
}
