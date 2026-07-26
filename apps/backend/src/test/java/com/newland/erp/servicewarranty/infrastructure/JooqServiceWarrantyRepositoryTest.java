package com.newland.erp.servicewarranty.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.newland.erp.servicewarranty.domain.ServiceTicket;
import com.newland.erp.servicewarranty.domain.WarrantyPolicy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.springframework.dao.DataIntegrityViolationException;
import org.flywaydb.core.Flyway;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
final class JooqServiceWarrantyRepositoryTest {
  private static final UUID COMPANY_ID =
      UUID.fromString("43000000-0000-4000-8000-000000000003");
  private static final UUID BRANCH_ID =
      UUID.fromString("43000000-0000-4000-8000-000000000004");
  private static final UUID PRODUCT_ID =
      UUID.fromString("43000000-0000-4000-8000-000000000005");
  private static final Instant NOW = Instant.parse("2026-07-26T00:00:00Z");

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17-alpine");

  private Connection connection;
  private JooqServiceWarrantyRepository repository;

  @BeforeEach
  void setUp() throws Exception {
    final Flyway flyway =
        Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .locations("classpath:db/migration")
            .cleanDisabled(false)
            .load();
    flyway.clean();
    flyway.migrate();
    connection = newConnection();
    seedEnterprise(connection);
    repository = new JooqServiceWarrantyRepository(DSL.using(connection));
  }

  @AfterEach
  void close() throws Exception {
    if (connection != null) {
      connection.close();
    }
  }

  @Test
  void persistsPolicyTicketDecisionDiagnosisAndResolution() {
    final WarrantyPolicy policy = policy();
    repository.insertPolicy(policy);
    ServiceTicket ticket = ticket(UUID.randomUUID(), "ticket-key");
    assertThat(repository.insertTicketIfAbsent(ticket)).isTrue();
    ticket = repository.updateTicket(ticket.beginValidation(NOW.plusSeconds(1)));
    ticket =
        repository.updateTicket(
            ticket.recordWarranty(
                new ServiceTicket.WarrantyDecision(
                    UUID.randomUUID(), policy.id(), true, "Covered",
                    LocalDate.of(2027, 1, 1), NOW.plusSeconds(2), "actor"),
                NOW.plusSeconds(2)));
    ticket =
        repository.updateTicket(
            ticket.diagnose(
                new ServiceTicket.Diagnosis(
                    UUID.randomUUID(), "Failed board", "Repair", NOW.plusSeconds(3)),
                NOW.plusSeconds(3)));
    ticket =
        repository.updateTicket(
            ticket.approveResolution(
                ServiceTicket.Resolution.Type.REPAIR, "Approved", NOW.plusSeconds(4)));
    ticket = repository.updateTicket(ticket.close("Completed", NOW.plusSeconds(5)));

    final ServiceTicket persisted = repository.findTicket(ticket.id()).orElseThrow();
    assertThat(persisted.status()).isEqualTo(ServiceTicket.Status.CLOSED);
    assertThat(persisted.warrantyDecision().eligible()).isTrue();
    assertThat(persisted.diagnosis().findings()).isEqualTo("Failed board");
    assertThat(persisted.resolution().outcome()).isEqualTo("Completed");
  }

  @Test
  void resolvesProductPolicyBeforeCompanyDefault() {
    final WarrantyPolicy global =
        new WarrantyPolicy(
            UUID.randomUUID(), COMPANY_ID, null, 90, false, false,
            LocalDate.of(2026, 1, 1), null, true);
    final WarrantyPolicy product = policy();
    repository.insertPolicy(global);
    repository.insertPolicy(product);

    assertThat(repository.resolvePolicy(COMPANY_ID, PRODUCT_ID, LocalDate.of(2026, 7, 1)))
        .get()
        .extracting(WarrantyPolicy::id)
        .isEqualTo(product.id());
  }

  @Test
  void rejectsOverlappingProductAndCompanyDefaultPolicies() {
    final WarrantyPolicy productPolicy = policy();
    repository.insertPolicy(productPolicy);
    final WarrantyPolicy overlappingProduct =
        new WarrantyPolicy(
            UUID.randomUUID(),
            COMPANY_ID,
            PRODUCT_ID,
            180,
            false,
            false,
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2027, 1, 1),
            true);

    assertThat(repository.hasOverlappingPolicy(overlappingProduct)).isTrue();
    assertThatThrownBy(() -> repository.insertPolicy(overlappingProduct))
        .isInstanceOf(DataIntegrityViolationException.class);

    final WarrantyPolicy companyDefault =
        new WarrantyPolicy(
            UUID.randomUUID(),
            COMPANY_ID,
            null,
            90,
            false,
            false,
            LocalDate.of(2026, 1, 1),
            null,
            true);
    repository.insertPolicy(companyDefault);
    final WarrantyPolicy overlappingDefault =
        new WarrantyPolicy(
            UUID.randomUUID(),
            COMPANY_ID,
            null,
            120,
            false,
            false,
            LocalDate.of(2026, 2, 1),
            null,
            true);

    assertThatThrownBy(() -> repository.insertPolicy(overlappingDefault))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void concurrentOverlappingPolicyCreationPersistsOnlyOnePolicy() throws Exception {
    final WarrantyPolicy first = policy();
    final WarrantyPolicy second =
        new WarrantyPolicy(
            UUID.randomUUID(),
            COMPANY_ID,
            PRODUCT_ID,
            180,
            false,
            false,
            LocalDate.of(2026, 6, 1),
            null,
            true);
    try (var executor = Executors.newFixedThreadPool(2)) {
      final List<Callable<Boolean>> calls =
          List.of(() -> insertPolicyUsingNewConnection(first), () -> insertPolicyUsingNewConnection(second));

      assertThat(executor.invokeAll(calls))
          .extracting(result -> result.get())
          .containsExactlyInAnyOrder(true, false);
    }
  }

  @Test
  void rollbackRemovesTicketAndRetrySucceeds() throws Exception {
    final ServiceTicket ticket = ticket(UUID.randomUUID(), "rollback-key");
    connection.setAutoCommit(false);
    assertThat(repository.insertTicketIfAbsent(ticket)).isTrue();
    connection.rollback();
    connection.setAutoCommit(true);

    assertThat(repository.findTicketByIdempotencyKey("rollback-key")).isEmpty();
    assertThat(repository.insertTicketIfAbsent(ticket)).isTrue();
  }

  @Test
  void concurrentIdempotentCreationPersistsOneTicket() throws Exception {
    final ServiceTicket first = ticket(UUID.randomUUID(), "concurrent-key");
    final ServiceTicket second = ticket(UUID.randomUUID(), "concurrent-key");
    try (var executor = Executors.newFixedThreadPool(2)) {
      final List<Callable<Boolean>> calls =
          List.of(() -> insertUsingNewConnection(first), () -> insertUsingNewConnection(second));
      final var results = executor.invokeAll(calls);
      assertThat(results)
          .extracting(result -> result.get())
          .containsExactlyInAnyOrder(true, false);
      assertThat(repository.findTicketByIdempotencyKey("concurrent-key")).isPresent();
    }
  }

  @Test
  void optimisticLockRejectsStaleTicketUpdate() {
    final ServiceTicket ticket = ticket(UUID.randomUUID(), "lock-key");
    repository.insertTicketIfAbsent(ticket);
    repository.updateTicket(ticket.beginValidation(NOW.plusSeconds(1)));

    assertThatThrownBy(
            () -> repository.updateTicket(ticket.beginValidation(NOW.plusSeconds(2))))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("concurrently");
  }

  private boolean insertUsingNewConnection(final ServiceTicket ticket) throws SQLException {
    try (Connection concurrent = newConnection()) {
      return new JooqServiceWarrantyRepository(DSL.using(concurrent))
          .insertTicketIfAbsent(ticket);
    }
  }

  private boolean insertPolicyUsingNewConnection(final WarrantyPolicy policy) throws SQLException {
    try (Connection concurrent = newConnection()) {
      try {
        new JooqServiceWarrantyRepository(DSL.using(concurrent)).insertPolicy(policy);
        return true;
      } catch (DataIntegrityViolationException exception) {
        return false;
      }
    }
  }

  private Connection newConnection() throws SQLException {
    return DriverManager.getConnection(
        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
  }

  private static WarrantyPolicy policy() {
    return new WarrantyPolicy(
        UUID.randomUUID(), COMPANY_ID, PRODUCT_ID, 365, true, true,
        LocalDate.of(2026, 1, 1), null, true);
  }

  private static ServiceTicket ticket(final UUID id, final String key) {
    return new ServiceTicket(
        id, key, "SRV-" + id, COMPANY_ID, BRANCH_ID, UUID.randomUUID(), PRODUCT_ID,
        UUID.randomUUID(), "SERIAL-1", UUID.randomUUID(), LocalDate.of(2026, 1, 1),
        "Device will not start", ServiceTicket.Status.OPEN, null, null, null, 0, NOW, NOW,
        "actor");
  }

  private static void seedEnterprise(final Connection target) throws SQLException {
    target
        .createStatement()
        .executeUpdate(
            """
            INSERT INTO enterprise
              (id, code, name, status, created_at, created_by, updated_at, updated_by)
            VALUES ('43000000-0000-4000-8000-000000000001', 'ENT', 'Enterprise', 'ACTIVE',
                    now(), 'test', now(), 'test');
            INSERT INTO legal_entity
              (id, enterprise_id, code, name, country_code, base_currency, status,
               created_at, created_by, updated_at, updated_by)
            VALUES ('43000000-0000-4000-8000-000000000002',
                    '43000000-0000-4000-8000-000000000001', 'LE', 'Legal Entity', 'CN', 'USD',
                    'ACTIVE', now(), 'test', now(), 'test');
            INSERT INTO company
              (id, enterprise_id, legal_entity_id, code, name, country_code, base_currency,
               time_zone_id, status, created_at, created_by, updated_at, updated_by)
            VALUES ('43000000-0000-4000-8000-000000000003',
                    '43000000-0000-4000-8000-000000000001',
                    '43000000-0000-4000-8000-000000000002', 'COMP', 'Company', 'CN', 'USD',
                    'Asia/Shanghai', 'ACTIVE', now(), 'test', now(), 'test');
            INSERT INTO branch
              (id, enterprise_id, company_id, code, name, status,
               created_at, created_by, updated_at, updated_by)
            VALUES ('43000000-0000-4000-8000-000000000004',
                    '43000000-0000-4000-8000-000000000001',
                    '43000000-0000-4000-8000-000000000003', 'BR', 'Branch', 'ACTIVE',
                    now(), 'test', now(), 'test');
            """);
  }
}
