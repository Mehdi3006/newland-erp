package com.newland.erp.inventory.infrastructure;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
final class InventoryMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Test
    void migratesInventorySchemaWithoutAdjacentBusinessModules() throws Exception {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(),
                POSTGRES.getPassword());
             var statement = connection.createStatement();
             var resultSet = statement.executeQuery("""
                     select
                       count(*) filter (where table_name like 'inventory_%') as inventory_tables,
                       count(*) filter (where table_name in ('customer_payment', 'sales_invoice', 'price_list',
                                                            'journal_entry', 'manufacturing_order', 'crm_account',
                                                            'hr_employee')) as forbidden_tables
                     from information_schema.tables
                     where table_schema = 'public'
                     """)) {
            resultSet.next();
            assertThat(resultSet.getInt("inventory_tables")).isEqualTo(7);
            assertThat(resultSet.getInt("forbidden_tables")).isZero();
        }
    }
}
