package com.newland.erp.logistics.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.newland.erp.logistics.domain.LandedCostDraft;
import com.newland.erp.logistics.domain.Shipment;
import com.newland.erp.logistics.domain.ShipmentTest;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17-alpine");

  private Connection connection;
  private JooqLogisticsRepository repository;

  @BeforeEach
  void setUp() throws Exception {
    Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .cleanDisabled(false)
        .load()
        .clean();
    Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .load()
        .migrate();
    connection =
        DriverManager.getConnection(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    connection.createStatement().execute("set session_replication_role = replica");
    repository = new JooqLogisticsRepository(DSL.using(connection));
  }

  @AfterEach
  void close() throws Exception {
    if (connection != null) {
      connection.close();
    }
  }

  @Test
  void persistsShipmentContainersMilestonesAndLandedCostDraft() {
    final Shipment draft = ShipmentTest.shipment();
    repository.insertShipment(draft);
    final Shipment.Container container =
        new Shipment.Container(
            UUID.randomUUID(), "MSCU-100", "40HC", new BigDecimal("1000"),
            new BigDecimal("60"), null);
    final Shipment booked = repository.updateShipment(draft.book());
    final Shipment loaded =
        repository.updateShipment(
            repository
                .updateShipment(booked.addContainer(container))
                .loadContainer(container.id(), Instant.now()));
    final Shipment released =
        repository.updateShipment(
            loaded.recordMilestone(
                new Shipment.CustomsMilestone(
                    UUID.randomUUID(), Shipment.MilestoneType.CUSTOMS_RELEASED,
                    "REL-100", Instant.now(), "released")));
    final LandedCostDraft cost =
        new LandedCostDraft(
            UUID.randomUUID(), draft.id(), "cost-100", "USD",
            LandedCostDraft.AllocationBasis.WEIGHT,
            List.of(
                new LandedCostDraft.CostComponent(
                    UUID.randomUUID(), "FREIGHT", new BigDecimal("125.50"), "BL-100")),
            Instant.now(), "actor");
    repository.insertLandedCostDraft(cost);

    assertThat(repository.findShipment(draft.id()))
        .get()
        .satisfies(
            persisted -> {
              assertThat(persisted.status()).isEqualTo(released.status());
              assertThat(persisted.containers()).hasSize(1);
              assertThat(persisted.milestones()).hasSize(1);
            });
    assertThat(repository.findLandedCostDraft(cost.id()))
        .get()
        .extracting(LandedCostDraft::total)
        .isEqualTo(new BigDecimal("125.500000"));
  }

  @Test
  void optimisticLockRejectsStaleShipmentUpdate() {
    final Shipment draft = ShipmentTest.shipment();
    repository.insertShipment(draft);
    repository.updateShipment(draft.book());

    assertThatThrownBy(() -> repository.updateShipment(draft.book()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("concurrently");
  }
}
