package com.newland.erp.servicewarranty.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
final class ServiceWarrantyMigrationTest {
  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17-alpine");

  @Test
  void createsOnlyApprovedServiceWarrantyTables() throws Exception {
    Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .load()
        .migrate();
    try (var connection =
            DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        var statement = connection.createStatement();
        var tables =
            statement.executeQuery(
                """
                select table_name from information_schema.tables
                where table_schema = 'public'
                  and (table_name like 'service_%')
                order by table_name
                """)) {
      final var names = new java.util.ArrayList<String>();
      while (tables.next()) {
        names.add(tables.getString("table_name"));
      }
      assertThat(names)
          .containsExactly(
              "service_diagnosis",
              "service_resolution",
              "service_ticket",
              "service_warranty_decision",
              "service_warranty_policy");
    }
  }
}
