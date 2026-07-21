package com.newland.erp.sales.infrastructure;

import com.newland.erp.sales.domain.Customer;
import com.newland.erp.sales.domain.CustomerStatus;
import com.newland.erp.sales.domain.SalesConflictException;
import com.newland.erp.sales.domain.SalesLine;
import com.newland.erp.sales.domain.SalesOrder;
import com.newland.erp.sales.domain.SalesOrderStatus;
import com.newland.erp.sales.domain.SalesQuantity;
import com.newland.erp.sales.domain.SalesQuotation;
import com.newland.erp.sales.domain.SalesQuotationStatus;

import org.flywaydb.core.Flyway;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
final class JooqSalesRepositoryTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    private JooqSalesRepository repository;

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
        repository = new JooqSalesRepository(DSL.using(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(),
                POSTGRES.getPassword()));
    }

    @Test
    void rejectsStaleQuotationAndOrderUpdates() {
        final Customer customer = repository.insertCustomer(customer());
        final SalesQuotation quotation = repository.insertQuotation(quotation(customer.id()));
        repository.updateQuotation(quotation.approve());

        assertThatThrownBy(() -> repository.updateQuotation(quotation.reject()))
                .isInstanceOf(SalesConflictException.class);

        final SalesOrder order = repository.insertSalesOrder(order(customer.id()));
        repository.updateSalesOrder(order.approve());

        assertThatThrownBy(() -> repository.updateSalesOrder(order.cancel()))
                .isInstanceOf(SalesConflictException.class);
    }

    private static Customer customer() {
        return new Customer(UUID.randomUUID(), "customer-lock", "CUS-LOCK", "Lock Customer",
                CustomerStatus.ACTIVE, List.of(), List.of(), List.of(), List.of(), Instant.now());
    }

    private static SalesQuotation quotation(final UUID customerId) {
        return new SalesQuotation(UUID.randomUUID(), "SQ-LOCK", "quotation-lock", customerId, UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), SalesQuotationStatus.SUBMITTED, 0, List.of(line()), 0,
                LocalDate.parse("2026-12-31"), Instant.now(), "architect");
    }

    private static SalesOrder order(final UUID customerId) {
        final SalesQuantity zero = quantity("0");
        return new SalesOrder(UUID.randomUUID(), "SO-LOCK", "order-lock", null, customerId, UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), SalesOrderStatus.DRAFT,
                0, List.of(new SalesOrder.SalesOrderLine(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "SKU-LOCK", quantity("1"), zero, zero, zero, UUID.randomUUID())), 0,
                LocalDate.parse("2026-12-31"), Instant.now(), "architect");
    }

    private static SalesLine line() {
        return new SalesLine(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "SKU-LOCK", quantity("1"),
                BigDecimal.TEN, UUID.randomUUID());
    }

    private static SalesQuantity quantity(final String value) {
        return new SalesQuantity(new BigDecimal(value), "EA");
    }
}
