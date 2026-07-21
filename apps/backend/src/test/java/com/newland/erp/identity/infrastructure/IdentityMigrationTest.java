package com.newland.erp.identity.infrastructure;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
final class IdentityMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Test
    void migratesIdentitySchemaWithoutHrOrBusinessModuleTables() throws Exception {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        );
             var statement = connection.createStatement();
             var resultSet = statement.executeQuery("""
                     select table_name
                     from information_schema.tables
                     where table_schema = 'public'
                     order by table_name
                     """)) {
            final Set<String> forbidden = Set.of("employee", "customer_payment", "supplier_payment",
                    "inventory_item", "journal_entry", "sales_invoice");
            while (resultSet.next()) {
                assertThat(resultSet.getString("table_name")).isNotIn(forbidden);
            }
        }
    }
}
