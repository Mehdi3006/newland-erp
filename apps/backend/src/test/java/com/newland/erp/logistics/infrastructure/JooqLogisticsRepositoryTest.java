package com.newland.erp.logistics.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.newland.erp.logistics.domain.LandedCostDraft;
import com.newland.erp.logistics.domain.Shipment;
import com.newland.erp.masterdata.application.integration.MasterDataReferencePort;
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
final class JooqLogisticsRepositoryTest {
  private static final UUID ENTERPRISE_ID =
      UUID.fromString("41000000-0000-4000-8000-000000000001");
  private static final UUID LEGAL_ENTITY_ID =
      UUID.fromString("41000000-0000-4000-8000-000000000002");
  private static final UUID COMPANY_ID =
      UUID.fromString("41000000-0000-4000-8000-000000000003");
  private static final UUID BRANCH_ID =
      UUID.fromString("41000000-0000-4000-8000-000000000004");
  private static final UUID WAREHOUSE_ID =
      UUID.fromString("41000000-0000-4000-8000-000000000005");
  private static final UUID SUPPLIER_ID =
      UUID.fromString("41000000-0000-4000-8000-000000000006");
  private static final UUID PURCHASE_ORDER_ID =
      UUID.fromString("41000000-0000-4000-8000-000000000007");

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17-alpine");

  private Connection connection;
  private JooqLogisticsRepository repository;

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
    repository = new JooqLogisticsRepository(DSL.using(connection));
  }

  @AfterEach
  void close() throws Exception {
    if (connection != null) {
      connection.close();
    }
  }

  @Test
  void persistsUsingRealForeignKeysAndRejectsInvalidProcurementReference() {
    final Shipment draft = shipment(UUID.randomUUID(), "shipment-key", PURCHASE_ORDER_ID);

    assertThat(repository.insertShipmentIfAbsent(draft)).isTrue();
    assertThat(repository.findShipment(draft.id())).contains(draft);

    final Shipment invalid =
        shipment(UUID.randomUUID(), "invalid-po-key", UUID.randomUUID());
    assertThatThrownBy(() -> repository.insertShipmentIfAbsent(invalid))
        .isInstanceOf(org.jooq.exception.DataAccessException.class)
        .hasMessageContaining("fk");
  }

  @Test
  void rollsBackAtomicallyAndAllowsSafeRetry() throws Exception {
    final Shipment draft = shipment(UUID.randomUUID(), "rollback-key", PURCHASE_ORDER_ID);
    connection.setAutoCommit(false);
    assertThat(repository.insertShipmentIfAbsent(draft)).isTrue();
    connection.rollback();
    connection.setAutoCommit(true);

    assertThat(repository.findShipmentByIdempotencyKey(draft.idempotencyKey())).isEmpty();
    assertThat(repository.insertShipmentIfAbsent(draft)).isTrue();
    assertThat(repository.findShipmentByIdempotencyKey(draft.idempotencyKey())).contains(draft);
  }

  @Test
  void concurrentShipmentCreationIsAtomicallyIdempotent() throws Exception {
    final String key = "concurrent-shipment-key";
    final Shipment first = shipment(UUID.randomUUID(), key, PURCHASE_ORDER_ID);
    final Shipment second = shipment(UUID.randomUUID(), key, PURCHASE_ORDER_ID);
    try (var executor = Executors.newFixedThreadPool(2)) {
      final List<Callable<Boolean>> calls =
          List.of(
              () -> insertUsingNewConnection(first),
              () -> insertUsingNewConnection(second));
      final var results = executor.invokeAll(calls);

      assertThat(results)
          .extracting(result -> result.get())
          .containsExactlyInAnyOrder(true, false);
      assertThat(repository.findShipmentByIdempotencyKey(key)).isPresent();
      assertThat(
              DSL.using(connection)
                  .fetchCount(
                      DSL.table("logistics_shipment"),
                      DSL.field("idempotency_key", String.class).eq(key)))
          .isEqualTo(1);
    }
  }

  @Test
  void landedCostCreationIsAtomicAndPreservesComponents() {
    final Shipment shipment = shipment(UUID.randomUUID(), "cost-shipment", PURCHASE_ORDER_ID);
    repository.insertShipmentIfAbsent(shipment);
    final LandedCostDraft first = landedCost(shipment.id(), UUID.randomUUID(), "cost-key");
    final LandedCostDraft duplicate = landedCost(shipment.id(), UUID.randomUUID(), "cost-key");

    assertThat(repository.insertLandedCostDraftIfAbsent(first)).isTrue();
    assertThat(repository.insertLandedCostDraftIfAbsent(duplicate)).isFalse();
    assertThat(repository.findLandedCostDraftByIdempotencyKey("cost-key"))
        .get()
        .satisfies(
            persisted -> {
              assertThat(persisted.id()).isEqualTo(first.id());
              assertThat(persisted.components()).hasSize(1);
            });
  }

  @Test
  void concurrentLandedCostCreationIsAtomicallyIdempotent() throws Exception {
    final Shipment shipment =
        shipment(UUID.randomUUID(), "concurrent-cost-shipment", PURCHASE_ORDER_ID);
    repository.insertShipmentIfAbsent(shipment);
    final LandedCostDraft first = landedCost(shipment.id(), UUID.randomUUID(), "concurrent-cost");
    final LandedCostDraft second = landedCost(shipment.id(), UUID.randomUUID(), "concurrent-cost");
    try (var executor = Executors.newFixedThreadPool(2)) {
      final var results =
          executor.invokeAll(
              List.of(
                  () -> insertLandedCostUsingNewConnection(first),
                  () -> insertLandedCostUsingNewConnection(second)));

      assertThat(results)
          .extracting(result -> result.get())
          .containsExactlyInAnyOrder(true, false);
      assertThat(repository.findLandedCostDraftByIdempotencyKey("concurrent-cost")).isPresent();
    }
  }

  @Test
  void validatesPersistentCarrierPortAndMasterDataIncotermReferences() throws Exception {
    connection
        .createStatement()
        .executeUpdate(
            """
            INSERT INTO logistics_carrier (id, code, display_name, active)
            VALUES (gen_random_uuid(), 'ACTIVE-CARRIER', 'Active Carrier', true),
                   (gen_random_uuid(), 'INACTIVE-CARRIER', 'Inactive Carrier', false);
            INSERT INTO logistics_port (id, code, display_name, country_code, active)
            VALUES (gen_random_uuid(), 'CNSZX', 'Shenzhen', 'CN', true);
            """);
    final MasterDataReferencePort masterData =
        new MasterDataReferencePort() {
          @Override
          public boolean isActiveCurrency(final String currencyCode) {
            return false;
          }

          @Override
          public boolean isActiveReference(final String type, final String code) {
            return type.equals("incoterms") && code.equals("FOB");
          }

          @Override
          public java.util.Optional<ExchangeRateSnapshot> resolveExchangeRate(
              final UUID companyId,
              final String sourceCurrency,
              final String targetCurrency,
              final LocalDate effectiveDate) {
            return java.util.Optional.empty();
          }
        };
    final var references =
        new JooqLogisticsMasterReferenceAdapter(DSL.using(connection), masterData);

    references.requireActiveCarrier("active-carrier");
    references.requireActivePort("cnszx");
    references.requireActiveIncoterm("FOB");
    assertThatThrownBy(() -> references.requireActiveCarrier("INACTIVE-CARRIER"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> references.requireActivePort("UNKNOWN"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> references.requireActiveIncoterm("UNKNOWN"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void optimisticLockRejectsStaleShipmentUpdate() {
    final Shipment draft = shipment(UUID.randomUUID(), "lock-key", PURCHASE_ORDER_ID);
    repository.insertShipmentIfAbsent(draft);
    repository.updateShipment(draft.book());

    assertThatThrownBy(() -> repository.updateShipment(draft.book()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("concurrently");
  }

  private boolean insertUsingNewConnection(final Shipment shipment) throws SQLException {
    try (Connection concurrent = newConnection()) {
      return new JooqLogisticsRepository(DSL.using(concurrent))
          .insertShipmentIfAbsent(shipment);
    }
  }

  private boolean insertLandedCostUsingNewConnection(final LandedCostDraft draft)
      throws SQLException {
    try (Connection concurrent = newConnection()) {
      return new JooqLogisticsRepository(DSL.using(concurrent))
          .insertLandedCostDraftIfAbsent(draft);
    }
  }

  private Connection newConnection() throws SQLException {
    return DriverManager.getConnection(
        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
  }

  private static Shipment shipment(
      final UUID shipmentId, final String idempotencyKey, final UUID purchaseOrderId) {
    return new Shipment(
        shipmentId,
        "SHP-" + shipmentId,
        idempotencyKey,
        purchaseOrderId,
        SUPPLIER_ID,
        COMPANY_ID,
        BRANCH_ID,
        WAREHOUSE_ID,
        "CARRIER",
        "CNSZX",
        "IRBND",
        "FOB",
        LocalDate.of(2026, 8, 1),
        LocalDate.of(2026, 8, 20),
        Shipment.Status.DRAFT,
        List.of(),
        List.of(),
        0,
        Instant.parse("2026-07-26T00:00:00Z"),
        "actor");
  }

  private static LandedCostDraft landedCost(
      final UUID shipmentId, final UUID draftId, final String key) {
    return new LandedCostDraft(
        draftId,
        shipmentId,
        key,
        "USD",
        LandedCostDraft.AllocationBasis.WEIGHT,
        List.of(
            new LandedCostDraft.CostComponent(
                UUID.randomUUID(), "FREIGHT", new BigDecimal("125.50"), "BL-100")),
        Instant.parse("2026-07-26T00:00:00Z"),
        "actor");
  }

  private static void seedRequiredReferences(final Connection target) throws SQLException {
    target
        .createStatement()
        .executeUpdate(
            """
            INSERT INTO enterprise
              (id, code, name, status, created_at, created_by, updated_at, updated_by)
            VALUES ('41000000-0000-4000-8000-000000000001', 'ENT', 'Enterprise', 'ACTIVE',
                    now(), 'test', now(), 'test');
            INSERT INTO legal_entity
              (id, enterprise_id, code, name, country_code, base_currency, status,
               created_at, created_by, updated_at, updated_by)
            VALUES ('41000000-0000-4000-8000-000000000002',
                    '41000000-0000-4000-8000-000000000001', 'LE', 'Legal Entity', 'CN', 'USD',
                    'ACTIVE', now(), 'test', now(), 'test');
            INSERT INTO company
              (id, enterprise_id, legal_entity_id, code, name, country_code, base_currency,
               time_zone_id, status, created_at, created_by, updated_at, updated_by)
            VALUES ('41000000-0000-4000-8000-000000000003',
                    '41000000-0000-4000-8000-000000000001',
                    '41000000-0000-4000-8000-000000000002', 'COMP', 'Company', 'CN', 'USD',
                    'Asia/Shanghai', 'ACTIVE', now(), 'test', now(), 'test');
            INSERT INTO branch
              (id, enterprise_id, company_id, code, name, status,
               created_at, created_by, updated_at, updated_by)
            VALUES ('41000000-0000-4000-8000-000000000004',
                    '41000000-0000-4000-8000-000000000001',
                    '41000000-0000-4000-8000-000000000003', 'BR', 'Branch', 'ACTIVE',
                    now(), 'test', now(), 'test');
            INSERT INTO warehouse
              (id, enterprise_id, company_id, branch_id, code, name, warehouse_type, status,
               created_at, created_by, updated_at, updated_by)
            VALUES ('41000000-0000-4000-8000-000000000005',
                    '41000000-0000-4000-8000-000000000001',
                    '41000000-0000-4000-8000-000000000003',
                    '41000000-0000-4000-8000-000000000004', 'WH', 'Warehouse', 'BRANCH',
                    'ACTIVE', now(), 'test', now(), 'test');
            INSERT INTO procurement_supplier
              (id, idempotency_key, supplier_code, name, status, created_at)
            VALUES ('41000000-0000-4000-8000-000000000006',
                    'supplier-key', 'SUP', 'Supplier', 'ACTIVE', now());
            INSERT INTO procurement_purchase_order
              (id, order_number, idempotency_key, supplier_id, company_id, branch_id,
               warehouse_id, currency_id, status, revision, created_at, actor)
            VALUES ('41000000-0000-4000-8000-000000000007', 'PO-1', 'po-key',
                    '41000000-0000-4000-8000-000000000006',
                    '41000000-0000-4000-8000-000000000003',
                    '41000000-0000-4000-8000-000000000004',
                    '41000000-0000-4000-8000-000000000005',
                    '41000000-0000-4000-8000-000000000008',
                    'APPROVED', 0, now(), 'test');
            """);
  }
}
