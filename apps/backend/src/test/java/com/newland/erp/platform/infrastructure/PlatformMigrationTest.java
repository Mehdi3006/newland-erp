package com.newland.erp.platform.infrastructure;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
final class PlatformMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Test
    void migratesPlatformSchemaWithoutBusinessModuleTables() throws Exception {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(),
                POSTGRES.getPassword());
             var statement = connection.createStatement();
             var resultSet = statement.executeQuery("""
                     select count(*)
                     from information_schema.tables
                     where table_schema = 'public'
                       and table_name in ('inventory_item', 'customer_payment', 'supplier_payment',
                                          'sales_invoice', 'journal_entry')
                     """)) {
            resultSet.next();
            assertThat(resultSet.getInt(1)).isZero();
        }
    }
}
