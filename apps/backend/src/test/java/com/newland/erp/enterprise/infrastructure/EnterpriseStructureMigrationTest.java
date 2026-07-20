package com.newland.erp.enterprise.infrastructure;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
final class EnterpriseStructureMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Test
    void migratesEnterpriseStructureSchemaWithoutSeedData() throws Exception {
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
                var tables = connection.createStatement().executeQuery("""
                        select table_name
                        from information_schema.tables
                        where table_schema = 'public'
                        order by table_name
                        """);
                var rows = connection.createStatement().executeQuery("select count(*) from enterprise")) {
            final Set<String> tableNames = new TreeSet<>();
            while (tables.next()) {
                tableNames.add(tables.getString("table_name"));
            }

            rows.next();

            assertThat(tableNames).contains(
                    "enterprise",
                    "legal_entity",
                    "company",
                    "branch",
                    "warehouse",
                    "warehouse_zone",
                    "warehouse_location"
            );
            assertThat(rows.getLong(1)).isZero();
        }
    }
}
