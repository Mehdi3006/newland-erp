package com.newland.erp.crm.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.newland.erp.crm.domain.Activity;
import com.newland.erp.crm.domain.Lead;
import com.newland.erp.crm.domain.Opportunity;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.flywaydb.core.Flyway;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
final class JooqCrmRepositoryTest {
  private static final UUID COMPANY_ID =
      UUID.fromString("42000000-0000-4000-8000-000000000003");
  private static final UUID BRANCH_ID =
      UUID.fromString("42000000-0000-4000-8000-000000000004");
  private static final UUID USER_ID =
      UUID.fromString("42000000-0000-4000-8000-000000000005");
  private static final UUID CUSTOMER_ID =
      UUID.fromString("42000000-0000-4000-8000-000000000006");
  private static final Instant NOW = Instant.parse("2026-07-26T00:00:00Z");

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17-alpine");

  private Connection connection;
  private JooqCrmRepository repository;

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
    seedRequiredReferences(connection);
    repository = new JooqCrmRepository(DSL.using(connection));
  }

  @AfterEach
  void close() throws Exception {
    if (connection != null) {
      connection.close();
    }
  }

  @Test
  void persistsLeadOpportunityActivityAndCustomerTimeline() {
    final Lead lead = lead(UUID.randomUUID(), "lead-key");
    assertThat(repository.insertLeadIfAbsent(lead)).isTrue();
    final Lead qualified = repository.updateLead(lead.qualify(NOW.plusSeconds(1)));
    final Opportunity opportunity =
        opportunity(UUID.randomUUID(), "opportunity-key", qualified.id());
    assertThat(repository.insertOpportunityIfAbsent(opportunity)).isTrue();
    final Activity activity =
        activity(UUID.randomUUID(), "activity-key", qualified.id(), opportunity.id());
    assertThat(repository.insertActivityIfAbsent(activity)).isTrue();

    assertThat(repository.findLead(lead.id())).contains(qualified);
    assertThat(repository.findOpportunity(opportunity.id()))
        .get()
        .satisfies(
            persisted -> {
              assertThat(persisted.id()).isEqualTo(opportunity.id());
              assertThat(persisted.estimatedValue())
                  .isEqualByComparingTo(opportunity.estimatedValue());
              assertThat(persisted.stage()).isEqualTo(opportunity.stage());
            });
    assertThat(repository.listCustomerActivities(COMPANY_ID, CUSTOMER_ID))
        .containsExactly(activity);
  }

  @Test
  void enforcesRealCompanyBranchOwnerAndLeadForeignKeys() {
    final Lead invalid =
        new Lead(
            UUID.randomUUID(), "invalid", COMPANY_ID, UUID.randomUUID(), USER_ID, "LEAD-X",
            "Organization", "Contact", "test@example.com", "", "WEB", Lead.Status.NEW,
            "", 0, NOW, NOW, USER_ID.toString());

    assertThatThrownBy(() -> repository.insertLeadIfAbsent(invalid))
        .isInstanceOf(org.jooq.exception.DataAccessException.class)
        .hasMessageContaining("fk_crm_lead_branch_scope");
  }

  @Test
  void rollsBackAndAllowsIdempotentRetry() throws Exception {
    final Lead lead = lead(UUID.randomUUID(), "rollback-key");
    connection.setAutoCommit(false);
    assertThat(repository.insertLeadIfAbsent(lead)).isTrue();
    connection.rollback();
    connection.setAutoCommit(true);

    assertThat(repository.findLeadByIdempotencyKey("rollback-key")).isEmpty();
    assertThat(repository.insertLeadIfAbsent(lead)).isTrue();
  }

  @Test
  void concurrentDuplicateLeadCreatesOneDurableAggregate() throws Exception {
    final Lead first = lead(UUID.randomUUID(), "concurrent-key");
    final Lead second = lead(UUID.randomUUID(), "concurrent-key");
    try (var executor = Executors.newFixedThreadPool(2)) {
      final List<Callable<Boolean>> calls =
          List.of(() -> insertUsingNewConnection(first), () -> insertUsingNewConnection(second));
      final var results = executor.invokeAll(calls);

      assertThat(results)
          .extracting(result -> result.get())
          .containsExactlyInAnyOrder(true, false);
      assertThat(repository.findLeadByIdempotencyKey("concurrent-key")).isPresent();
    }
  }

  @Test
  void optimisticLockRejectsStaleLeadAndOpportunityUpdates() {
    final Lead lead = lead(UUID.randomUUID(), "lock-lead");
    repository.insertLeadIfAbsent(lead);
    repository.updateLead(lead.qualify(NOW.plusSeconds(1)));
    assertThatThrownBy(() -> repository.updateLead(lead.qualify(NOW.plusSeconds(2))))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("concurrently");

    final Opportunity opportunity =
        opportunity(UUID.randomUUID(), "lock-opportunity", lead.id());
    repository.insertOpportunityIfAbsent(opportunity);
    repository.updateOpportunity(
        opportunity.advance(Opportunity.Stage.DISCOVERY, "", NOW.plusSeconds(1)));
    assertThatThrownBy(
            () ->
                repository.updateOpportunity(
                    opportunity.advance(
                        Opportunity.Stage.DISCOVERY, "", NOW.plusSeconds(2))))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("concurrently");
  }

  private boolean insertUsingNewConnection(final Lead lead) throws SQLException {
    try (Connection concurrent = newConnection()) {
      return new JooqCrmRepository(DSL.using(concurrent)).insertLeadIfAbsent(lead);
    }
  }

  private Connection newConnection() throws SQLException {
    return DriverManager.getConnection(
        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
  }

  private static Lead lead(final UUID id, final String key) {
    return new Lead(
        id, key, COMPANY_ID, BRANCH_ID, USER_ID, "LEAD-" + id, "Organization", "Contact",
        "contact@example.com", "", "WEB", Lead.Status.NEW, "", 0, NOW, NOW,
        USER_ID.toString());
  }

  private static Opportunity opportunity(
      final UUID id, final String key, final UUID leadId) {
    return new Opportunity(
        id, key, COMPANY_ID, BRANCH_ID, USER_ID, leadId, CUSTOMER_ID, "OPP-" + id,
        "Opportunity", Opportunity.Stage.QUALIFICATION, new BigDecimal("1000"), "USD", 20,
        LocalDate.of(2026, 9, 1), "", 0, NOW, NOW, USER_ID.toString());
  }

  private static Activity activity(
      final UUID id, final String key, final UUID leadId, final UUID opportunityId) {
    return new Activity(
        id, key, COMPANY_ID, CUSTOMER_ID, leadId, opportunityId, Activity.Type.MEETING,
        "Discovery meeting", "Requirements captured", NOW, NOW.plusSeconds(3600),
        USER_ID.toString());
  }

  private static void seedRequiredReferences(final Connection target) throws SQLException {
    target
        .createStatement()
        .executeUpdate(
            """
            INSERT INTO enterprise
              (id, code, name, status, created_at, created_by, updated_at, updated_by)
            VALUES ('42000000-0000-4000-8000-000000000001', 'ENT', 'Enterprise', 'ACTIVE',
                    now(), 'test', now(), 'test');
            INSERT INTO legal_entity
              (id, enterprise_id, code, name, country_code, base_currency, status,
               created_at, created_by, updated_at, updated_by)
            VALUES ('42000000-0000-4000-8000-000000000002',
                    '42000000-0000-4000-8000-000000000001', 'LE', 'Legal Entity', 'CN', 'USD',
                    'ACTIVE', now(), 'test', now(), 'test');
            INSERT INTO company
              (id, enterprise_id, legal_entity_id, code, name, country_code, base_currency,
               time_zone_id, status, created_at, created_by, updated_at, updated_by)
            VALUES ('42000000-0000-4000-8000-000000000003',
                    '42000000-0000-4000-8000-000000000001',
                    '42000000-0000-4000-8000-000000000002', 'COMP', 'Company', 'CN', 'USD',
                    'Asia/Shanghai', 'ACTIVE', now(), 'test', now(), 'test');
            INSERT INTO branch
              (id, enterprise_id, company_id, code, name, status,
               created_at, created_by, updated_at, updated_by)
            VALUES ('42000000-0000-4000-8000-000000000004',
                    '42000000-0000-4000-8000-000000000001',
                    '42000000-0000-4000-8000-000000000003', 'BR', 'Branch', 'ACTIVE',
                    now(), 'test', now(), 'test');
            INSERT INTO iam_user
              (id, username, email, display_name, status, password_expires_at,
               created_at, updated_at)
            VALUES ('42000000-0000-4000-8000-000000000005', 'crm-user',
                    'crm@example.com', 'CRM User', 'ACTIVE', now() + interval '90 days',
                    now(), now());
            """);
  }
}
