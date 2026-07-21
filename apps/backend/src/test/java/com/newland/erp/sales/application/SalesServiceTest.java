package com.newland.erp.sales.application;

import com.newland.erp.sales.domain.Customer;
import com.newland.erp.sales.domain.CustomerStatus;
import com.newland.erp.sales.domain.SalesConflictException;
import com.newland.erp.sales.domain.SalesLine;
import com.newland.erp.sales.domain.SalesOrder;
import com.newland.erp.sales.domain.SalesOrderStatus;
import com.newland.erp.sales.domain.SalesQuantity;
import com.newland.erp.sales.domain.SalesQuotationStatus;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class SalesServiceTest {
    private final InMemorySalesRepository repository = new InMemorySalesRepository();
    private final List<UUID> availabilityChecks = new ArrayList<>();
    private final List<UUID> reservationRequests = new ArrayList<>();
    private final List<UUID> deliveryRequests = new ArrayList<>();
    private final SalesService service = service(true, false);

    @Test
    void customerCreationUniqueCodeStatusAndCreditProfileValidation() {
        final Customer customer = service.createCustomer(customer("customer-1", "CUS-1"));

        assertThat(customer.status()).isEqualTo(CustomerStatus.ACTIVE);
        assertThatThrownBy(() -> service.createCustomer(customer("customer-2", "CUS-1")))
                .isInstanceOf(SalesConflictException.class);
        assertThatThrownBy(() -> service.createCustomer(customer("customer-1", "CUS-2")))
                .isInstanceOf(SalesConflictException.class);
        assertThatThrownBy(() -> service.changeCustomerStatus(new SalesCommands.ChangeCustomerStatus(
                service.changeCustomerStatus(new SalesCommands.ChangeCustomerStatus(customer.id(),
                        CustomerStatus.BLOCKED, "architect")).id(), CustomerStatus.ACTIVE, "architect")))
                .isInstanceOf(SalesConflictException.class);
    }

    @Test
    void quotationApprovalRevisionExpiryAndUnauthorizedApprovalAreEnforced() {
        final Customer customer = service.createCustomer(customer("customer-1", "CUS-1"));
        final var quotation = service.createQuotation(quotation("quote-1", customer.id(),
                LocalDate.parse("2026-07-20")));

        assertThatThrownBy(() -> service(false, false).approveQuotation(new SalesCommands.ApproveQuotation(
                quotation.id(), "architect"))).isInstanceOf(IllegalArgumentException.class);
        final var approved = service.approveQuotation(new SalesCommands.ApproveQuotation(quotation.id(),
                "architect"));
        final var revised = service.reviseQuotation(new SalesCommands.ReviseQuotation(approved.id(),
                "customer requested pack change", List.of(line("8")), "architect"));
        final var expiringQuotation = service.approveQuotation(new SalesCommands.ApproveQuotation(
                service.createQuotation(quotation("quote-2", customer.id(), LocalDate.parse("2026-07-20"))).id(),
                "architect"));
        final var expired = service.expireQuotation(new SalesCommands.ExpireQuotation(expiringQuotation.id(),
                LocalDate.parse("2026-08-01"), "architect"));
        final var rejected = service.rejectQuotation(new SalesCommands.RejectQuotation(
                service.createQuotation(quotation("quote-3", customer.id(), LocalDate.parse("2026-09-01"))).id(),
                "architect"));

        assertThat(approved.status()).isEqualTo(SalesQuotationStatus.APPROVED);
        assertThat(revised.status()).isEqualTo(SalesQuotationStatus.DRAFT);
        assertThat(repository.listQuotationRevisions(approved.id())).hasSize(1);
        assertThat(expired.status()).isEqualTo(SalesQuotationStatus.EXPIRED);
        assertThat(rejected.status()).isEqualTo(SalesQuotationStatus.REJECTED);
        assertThatThrownBy(() -> service.approveQuotation(new SalesCommands.ApproveQuotation(rejected.id(),
                "architect"))).isInstanceOf(SalesConflictException.class);
    }

    @Test
    void quotationConversionDirectSalesAndOrderLifecycleAreEnforced() {
        final Customer customer = service.createCustomer(customer("customer-1", "CUS-1"));
        final var quotation = service.approveQuotation(new SalesCommands.ApproveQuotation(
                service.createQuotation(quotation("quote-1", customer.id(), LocalDate.parse("2026-09-01"))).id(),
                "architect"));
        final SalesOrder order = service.createSalesOrder(order("order-1", quotation.id(), false, false,
                customer.id(), "10"));
        final SalesOrder direct = service.createSalesOrder(order("direct-1", null, true, false, customer.id(),
                "3"));

        assertThat(availabilityChecks).hasSize(2);
        assertThat(order.quotationId()).isEqualTo(quotation.id());
        assertThat(direct.quotationId()).isNull();
        assertThatThrownBy(() -> service.createSalesOrder(order("order-2", quotation.id(), false, false,
                customer.id(), "11"))).isInstanceOf(SalesConflictException.class);
    }

    @Test
    void expiredQuotationRequiresOverrideForConversion() {
        final Customer customer = service.createCustomer(customer("customer-1", "CUS-1"));
        final var expired = service.expireQuotation(new SalesCommands.ExpireQuotation(
                service.approveQuotation(new SalesCommands.ApproveQuotation(
                        service.createQuotation(quotation("quote-1", customer.id(),
                                LocalDate.parse("2026-07-20"))).id(), "architect")).id(),
                LocalDate.parse("2026-08-01"), "architect"));

        assertThatThrownBy(() -> service.createSalesOrder(order("order-1", expired.id(), false, false,
                customer.id(), "10"))).isInstanceOf(SalesConflictException.class);
        assertThat(service.createSalesOrder(order("order-2", expired.id(), false, true, customer.id(), "10")).id())
                .isNotNull();
    }

    @Test
    void salesOrderApprovalAmendmentReservationDeliveryCancellationAndRollbackAreEnforced() {
        final Customer customer = service.createCustomer(customer("customer-1", "CUS-1"));
        final var quotation = service.approveQuotation(new SalesCommands.ApproveQuotation(
                service.createQuotation(quotation("quote-1", customer.id(), LocalDate.parse("2026-09-01"))).id(),
                "architect"));
        final SalesOrder approved = service.approveSalesOrder(new SalesCommands.ApproveSalesOrder(
                service.createSalesOrder(order("order-1", quotation.id(), false, false, customer.id(), "10")).id(),
                "architect"));
        final SalesOrder amended = service.amendSalesOrder(new SalesCommands.AmendSalesOrder(approved.id(),
                "delivery date changed", List.of(orderLine("10")), "architect"));
        final SalesOrder reapproved = service.approveSalesOrder(new SalesCommands.ApproveSalesOrder(amended.id(),
                "architect"));
        final UUID lineId = reapproved.lines().getFirst().id();
        final SalesOrder reserved = service.reserveInventory(new SalesCommands.ReserveInventory(reapproved.id(),
                lineId, qty("2"), "architect"));
        final SalesOrder delivered = service.trackDelivery(new SalesCommands.TrackDelivery(reserved.id(), lineId,
                qty("3"), "architect"));
        final SalesOrder cancelled = service.cancelSalesOrder(new SalesCommands.CancelSalesOrder(delivered.id(),
                "architect"));

        assertThat(repository.listOrderRevisions(approved.id())).hasSize(1);
        assertThat(reservationRequests).contains(lineId);
        assertThat(deliveryRequests).contains(lineId);
        assertThat(cancelled.status()).isEqualTo(SalesOrderStatus.CANCELLED);
        assertThat(cancelled.lines().getFirst().remainingQuantity().value()).isEqualByComparingTo("0");
        assertThatThrownBy(() -> service.cancelSalesOrder(new SalesCommands.CancelSalesOrder(cancelled.id(),
                "architect"))).isInstanceOf(SalesConflictException.class);
        assertThatThrownBy(() -> service(true, true).reserveInventory(new SalesCommands.ReserveInventory(
                reapproved.id(), UUID.randomUUID(), qty("1"), "architect"))).isInstanceOf(SalesConflictException.class);
    }

    @Test
    void invalidReferencesUnauthorizedScopeAndInventoryFailurePreventCreation() {
        final Customer customer = service.createCustomer(customer("customer-1", "CUS-1"));

        assertThatThrownBy(() -> service.createQuotation(new SalesCommands.CreateQuotation("bad-product",
                customer.id(), ids(), ids(), ids(), ids(), ids(), ids(), ids(), ids(), List.of(line(null, "SKU-1")),
                LocalDate.parse("2026-09-01"), List.of(), "architect"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service(false, false).createQuotation(quotation("quote-1", customer.id(),
                LocalDate.parse("2026-09-01")))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service(true, true).createSalesOrder(order("order-fail", null, true, false,
                customer.id(), "1"))).isInstanceOf(IllegalArgumentException.class);
        assertThat(repository.orders).isEmpty();
    }

    private SalesService service(final boolean authorized, final boolean inventoryFails) {
        final AtomicLong sequence = new AtomicLong();
        return new SalesService(repository, (productId, skuId, skuCode) -> {
            if (productId == null || skuId == null || skuCode == null) {
                throw new IllegalArgumentException("invalid product");
            }
        }, new AllowingMasterDataPort(), new TestEnterpriseScopePort(), new TestInventoryPort(inventoryFails),
                prefix -> prefix + "-" + sequence.incrementAndGet(),
                (actor, action, targetId) -> {
                }, (eventType, aggregateId) -> {
                }, (aggregateId, attachmentId) -> {
                }, new TestAuthorizationPort(authorized),
                Clock.fixed(Instant.parse("2026-07-21T00:00:00Z"), ZoneOffset.UTC));
    }

    private static SalesCommands.CreateCustomer customer(final String key, final String code) {
        return new SalesCommands.CreateCustomer(key, code, "Customer " + code, List.of(), List.of(),
                List.of(new Customer.CustomerCreditProfile(UUID.randomUUID(), ids(), ids(), BigDecimal.TEN, false)),
                List.of(new Customer.CustomerProductReference(UUID.randomUUID(), ids(), ids(), "CUS-SKU")),
                List.of(UUID.randomUUID()), "architect");
    }

    private static SalesCommands.CreateQuotation quotation(final String key, final UUID customerId,
                                                           final LocalDate expiresOn) {
        return new SalesCommands.CreateQuotation(key, customerId, ids(), ids(), ids(), ids(), ids(), ids(), ids(),
                ids(), List.of(line(ids(), "SKU-1")), expiresOn, List.of(UUID.randomUUID()), "architect");
    }

    private static SalesCommands.CreateSalesOrder order(final String key, final UUID quotationId,
                                                        final boolean directSales,
                                                        final boolean expiredOverride,
                                                        final UUID customerId, final String quantity) {
        return new SalesCommands.CreateSalesOrder(key, quotationId, directSales, expiredOverride, customerId, ids(),
                ids(), ids(), ids(), ids(), List.of(orderLine(quantity)), LocalDate.parse("2026-08-01"),
                List.of(UUID.randomUUID()), "architect");
    }

    private static SalesLine line(final UUID productId, final String skuCode) {
        return new SalesLine(UUID.randomUUID(), productId, ids(), skuCode, qty("10"), BigDecimal.TEN, ids());
    }

    private static SalesLine line(final String quantity) {
        return new SalesLine(UUID.randomUUID(), ids(), ids(), "SKU-1", qty(quantity), BigDecimal.TEN, ids());
    }

    private static SalesOrder.SalesOrderLine orderLine(final String quantity) {
        final SalesQuantity zero = qty("0");
        return new SalesOrder.SalesOrderLine(UUID.randomUUID(), ids(), ids(), "SKU-1", qty(quantity), zero, zero,
                zero, ids());
    }

    private static SalesQuantity qty(final String value) {
        return new SalesQuantity(new BigDecimal(value), "EA");
    }

    private static UUID ids() {
        return UUID.randomUUID();
    }

    private final class TestInventoryPort implements SalesPorts.InventoryPort {
        private final boolean fails;

        TestInventoryPort(final boolean shouldFail) {
            this.fails = shouldFail;
        }

        @Override
        public void checkAvailability(final UUID skuId, final UUID warehouseId, final SalesQuantity quantity) {
            if (fails) {
                throw new IllegalArgumentException("inventory unavailable");
            }
            availabilityChecks.add(skuId);
        }

        @Override
        public void requestReservation(final UUID salesOrderId, final UUID lineId, final SalesQuantity quantity) {
            if (fails) {
                throw new IllegalArgumentException("reservation unavailable");
            }
            reservationRequests.add(lineId);
        }

        @Override
        public void requestDelivery(final UUID salesOrderId, final UUID lineId, final SalesQuantity quantity) {
            if (fails) {
                throw new IllegalArgumentException("delivery unavailable");
            }
            deliveryRequests.add(lineId);
        }
    }

    private static final class AllowingMasterDataPort implements SalesPorts.MasterDataPort {
        @Override
        public void requireUom(final String uomCode) {
            if (uomCode == null) {
                throw new IllegalArgumentException("invalid UOM");
            }
        }

        @Override
        public void requireCurrency(final UUID currencyId) {
        }

        @Override
        public void requireTaxCategory(final UUID taxCategoryId) {
        }

        @Override
        public void requirePaymentTerms(final UUID paymentTermsId) {
        }

        @Override
        public void requireShippingMethod(final UUID shippingMethodId) {
        }

        @Override
        public void requireIncoterms(final UUID incotermsId) {
        }
    }

    private static final class TestEnterpriseScopePort implements SalesPorts.EnterpriseScopePort {
        @Override
        public void requireCompanyBranchWarehouse(final UUID companyId, final UUID branchId, final UUID warehouseId) {
            if (companyId == null || branchId == null || warehouseId == null) {
                throw new IllegalArgumentException("invalid warehouse scope");
            }
        }

        @Override
        public void requireSalesChannel(final UUID salesChannelId) {
            if (salesChannelId == null) {
                throw new IllegalArgumentException("invalid sales channel");
            }
        }
    }

    private record TestAuthorizationPort(boolean authorized) implements SalesPorts.AuthorizationPort {
        @Override
        public void requirePermission(final String actor, final String capability) {
            if (!authorized) {
                throw new IllegalArgumentException("unauthorized");
            }
        }

        @Override
        public void requireCustomerScope(final String actor, final UUID customerId) {
            requirePermission(actor, "customer");
        }
    }
}
