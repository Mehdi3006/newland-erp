package com.newland.erp.platform.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newland.erp.platform.application.OutboxDispatcher;
import com.newland.erp.platform.application.PlatformCommands;
import com.newland.erp.platform.application.PlatformService;
import com.newland.erp.platform.domain.OutboxStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
final class JooqOutboxDispatcherTest {
    private static final Instant NOW = Instant.parse("2026-07-26T00:00:00Z");

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    private DataSource dataSource;
    private DSLContext dsl;
    private JooqPlatformRepository repository;

    @BeforeEach
    void setUp() {
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
        final DriverManagerDataSource physical = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        dataSource = new TransactionAwareDataSourceProxy(physical);
        dsl = DSL.using(dataSource, org.jooq.SQLDialect.POSTGRES);
        repository = new JooqPlatformRepository(dsl, new ObjectMapper());
    }

    @Test
    void rollbackDoesNotExposeOrPersistEvent() {
        final AtomicInteger delivered = new AtomicInteger();
        final PlatformService service = service(delivered);
        final TransactionTemplate transaction = transaction();

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            service.publishEvent(command());
            throw new IllegalStateException("rollback");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(delivered).hasValue(0);
        assertThat(dsl.fetchCount(DSL.table("platform_outbox"))).isZero();
    }

    @Test
    void retrySurvivesDispatcherRestartAndDoesNotDuplicateDelivery() {
        final AtomicInteger attempts = new AtomicInteger();
        service(new AtomicInteger()).publishEvent(command());
        final OutboxDispatcher failing = dispatcher(event -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("temporary");
        }, NOW);

        assertThat(failing.dispatchBatch(10)).isEqualTo(1);
        assertThat(repository.listPendingOutboxMessages(10))
                .singleElement()
                .extracting(message -> message.status())
                .isEqualTo(OutboxStatus.FAILED);

        final AtomicInteger delivered = new AtomicInteger();
        final OutboxDispatcher restarted = dispatcher(event -> delivered.incrementAndGet(),
                NOW.plusSeconds(2));
        assertThat(restarted.dispatchBatch(10)).isEqualTo(1);
        assertThat(restarted.dispatchBatch(10)).isZero();
        assertThat(attempts).hasValue(1);
        assertThat(delivered).hasValue(1);
    }

    @Test
    void concurrentDispatchersAtomicallyClaimMessageOnce() throws Exception {
        service(new AtomicInteger()).publishEvent(command());
        final AtomicInteger delivered = new AtomicInteger();
        final CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            final var first = executor.submit(() -> {
                start.await();
                return dispatcher(event -> delivered.incrementAndGet(), NOW).dispatchBatch(10);
            });
            final var second = executor.submit(() -> {
                start.await();
                return dispatcher(event -> delivered.incrementAndGet(), NOW).dispatchBatch(10);
            });
            start.countDown();
            assertThat(first.get() + second.get()).isEqualTo(1);
        }
        assertThat(delivered).hasValue(1);
    }

    private PlatformService service(final AtomicInteger delivered) {
        return new PlatformService(repository, event -> delivered.incrementAndGet(),
                new InMemoryStorage(), (jobId, jobType, scheduledAt, parameters) -> {},
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private OutboxDispatcher dispatcher(
            final com.newland.erp.platform.application.DomainEventBus bus, final Instant now) {
        return new OutboxDispatcher(
                new JooqPlatformRepository(dsl, new ObjectMapper()), bus,
                new DataSourceTransactionManager(dataSource), Clock.fixed(now, ZoneOffset.UTC));
    }

    private TransactionTemplate transaction() {
        return new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    private static PlatformCommands.PublishEvent command() {
        return new PlatformCommands.PublishEvent(
                "finance-posting", "FinancePostingCompleted", UUID.randomUUID(), Map.of());
    }

    private static final class InMemoryStorage
            implements com.newland.erp.platform.application.FileStoragePort {
        @Override
        public String put(final String storageKey, final byte[] content) {
            return storageKey;
        }

        @Override
        public byte[] get(final String storageKey) {
            return new byte[0];
        }
    }
}
