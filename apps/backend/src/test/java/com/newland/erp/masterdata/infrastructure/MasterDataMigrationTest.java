package com.newland.erp.masterdata.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newland.erp.masterdata.application.MasterDataCommands;
import com.newland.erp.masterdata.application.MasterDataService;
import com.newland.erp.masterdata.domain.MasterDataType;
import org.flywaydb.core.Flyway;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

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
                      and table_name in ('inventory_item', 'stock_movement', 'customer_payment', 'sales_invoice',
                                         'journal_entry', 'crm_account', 'employee')
                    """);
            assertThat(resultSet.next()).isFalse();
        }
    }

    @Test
    void resolvesAuthoritativeExchangeRateFromPostgresql() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
        final var repository = new JooqMasterDataRepository(
                DSL.using(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()),
                new ObjectMapper());
        final var service = new MasterDataService(repository,
                Clock.fixed(Instant.parse("2026-07-26T00:00:00Z"), ZoneOffset.UTC));
        final UUID companyId = UUID.randomUUID();
        service.create(new MasterDataCommands.Create(MasterDataType.EXCHANGE_RATE,
                "EUR-USD-" + companyId.toString().substring(0, 8), "EUR to USD", null,
                Map.of("companyId", companyId.toString(), "sourceCurrency", "EUR",
                        "targetCurrency", "USD", "validFrom", "2026-01-01",
                        "validTo", "2026-12-31", "rate", "1.12500000")));

        final var resolved = service.resolveExchangeRate(
                companyId, "EUR", "USD", LocalDate.parse("2026-07-26"));

        assertThat(resolved).isPresent();
        assertThat(resolved.orElseThrow().rate()).isEqualByComparingTo("1.12500000");
    }
}
