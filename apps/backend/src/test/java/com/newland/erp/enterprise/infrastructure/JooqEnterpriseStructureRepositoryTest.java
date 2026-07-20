package com.newland.erp.enterprise.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newland.erp.enterprise.domain.AuditMetadata;
import com.newland.erp.enterprise.domain.DisplayName;
import com.newland.erp.enterprise.domain.Enterprise;
import com.newland.erp.enterprise.domain.EnterpriseCode;
import com.newland.erp.enterprise.domain.LifecycleStatus;
import com.newland.erp.enterprise.domain.LocalizedName;
import com.newland.erp.enterprise.domain.OptimisticLockConflictException;

import org.flywaydb.core.Flyway;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
final class JooqEnterpriseStructureRepositoryTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Test
    void persistsReadsAndOptimisticallyUpdatesEnterprise() throws Exception {
        migrate();

        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        )) {
            final JooqEnterpriseStructureRepository repository =
                    new JooqEnterpriseStructureRepository(DSL.using(connection, SQLDialect.POSTGRES),
                            new ObjectMapper());
            final Enterprise enterprise = new Enterprise(UUID.randomUUID(), new EnterpriseCode("NL"),
                    new DisplayName("Newland"), new LocalizedName(Map.of("en", "Newland")),
                    LifecycleStatus.DRAFT, AuditMetadata.created(Instant.parse("2026-07-20T00:00:00Z"), "tester"));

            repository.insertEnterprise(enterprise);

            assertThat(repository.findEnterprise(enterprise.id())).contains(enterprise);
            assertThat(repository.enterpriseCodeExists(new EnterpriseCode("nl"))).isTrue();

            final Enterprise renamed = enterprise.rename(new DisplayName("Newland ERP"),
                    new LocalizedName(Map.of("en", "Newland ERP")),
                    enterprise.audit().touched(Instant.parse("2026-07-20T00:01:00Z"), "tester"));
            repository.updateEnterprise(renamed, 0L);

            assertThat(repository.findEnterprise(enterprise.id())).contains(renamed);
            assertThatThrownBy(() -> repository.updateEnterprise(renamed, 0L))
                    .isInstanceOf(OptimisticLockConflictException.class);
        }
    }

    private static void migrate() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .cleanDisabled(false)
                .load()
                .clean();
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }
}
