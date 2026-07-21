package com.newland.erp.masterdata.infrastructure;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
final class MasterDataMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Test
    void migratesMasterDataSchemaWithoutOperationalModuleTables() throws Exception {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(),
                POSTGRES.getPassword());
             var statement = connection.createStatement()) {
            final var resultSet = statement.executeQuery("""
                    select table_name
                    from information_schema.tables
                    where table_schema = 'public'
                      and table_name in ('inventory_item', 'stock_movement', 'sales_order', 'purchase_order',
                                         'journal_entry', 'crm_account', 'employee')
                    """);
            assertThat(resultSet.next()).isFalse();
        }
    }
}
