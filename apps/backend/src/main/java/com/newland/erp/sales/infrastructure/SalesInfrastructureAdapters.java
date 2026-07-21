package com.newland.erp.sales.infrastructure;

import com.newland.erp.sales.application.SalesPorts;
import com.newland.erp.sales.domain.SalesQuantity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public final class SalesInfrastructureAdapters {
    @Component
    public static final class ExistingProductCatalogAdapter implements SalesPorts.ProductCatalogPort {
        @Override
        public void requireSku(final UUID productId, final UUID skuId, final String skuCode) {
            requireId(productId, "product");
            requireId(skuId, "SKU");
            requireText(skuCode, "SKU code");
        }
    }

    @Component
    public static final class ExistingMasterDataAdapter implements SalesPorts.MasterDataPort {
        @Override
        public void requireUom(final String uomCode) {
            requireText(uomCode, "UOM");
        }

        @Override
        public void requireCurrency(final UUID currencyId) {
            requireId(currencyId, "currency");
        }

        @Override
        public void requireTaxCategory(final UUID taxCategoryId) {
            requireId(taxCategoryId, "tax category");
        }

        @Override
        public void requirePaymentTerms(final UUID paymentTermsId) {
            requireId(paymentTermsId, "payment terms");
        }

        @Override
        public void requireShippingMethod(final UUID shippingMethodId) {
            requireId(shippingMethodId, "shipping method");
        }

        @Override
        public void requireIncoterms(final UUID incotermsId) {
            requireId(incotermsId, "incoterms");
        }
    }

    @Component
    public static final class ExistingEnterpriseScopeAdapter implements SalesPorts.EnterpriseScopePort {
        @Override
        public void requireCompanyBranchWarehouse(final UUID companyId, final UUID branchId, final UUID warehouseId) {
            requireId(companyId, "company");
            requireId(branchId, "branch");
            requireId(warehouseId, "warehouse");
        }

        @Override
        public void requireSalesChannel(final UUID salesChannelId) {
            requireId(salesChannelId, "sales channel");
        }
    }

    @Component
    public static final class InventoryRequestAdapter implements SalesPorts.InventoryPort {
        private static final Logger LOGGER = LoggerFactory.getLogger(InventoryRequestAdapter.class);

        @Override
        public void checkAvailability(final UUID skuId, final UUID warehouseId, final SalesQuantity quantity) {
            requireId(skuId, "SKU");
            requireId(warehouseId, "warehouse");
            requireQuantity(quantity);
        }

        @Override
        public void requestReservation(final UUID salesOrderId, final UUID lineId, final SalesQuantity quantity) {
            requireId(salesOrderId, "sales order");
            requireId(lineId, "sales order line");
            requireQuantity(quantity);
            LOGGER.info("salesInventoryReservationRequested salesOrderId={} lineId={}", salesOrderId, lineId);
        }

        @Override
        public void requestDelivery(final UUID salesOrderId, final UUID lineId, final SalesQuantity quantity) {
            requireId(salesOrderId, "sales order");
            requireId(lineId, "sales order line");
            requireQuantity(quantity);
            LOGGER.info("salesDeliveryRequested salesOrderId={} lineId={}", salesOrderId, lineId);
        }
    }

    @Component
    public static final class SimpleSalesNumberSeriesAdapter implements SalesPorts.NumberSeriesPort {
        private final AtomicLong sequence = new AtomicLong();

        @Override
        public String nextNumber(final String prefix) {
            requireText(prefix, "number prefix");
            return prefix + "-" + sequence.incrementAndGet();
        }
    }

    @Component
    public static final class LoggingSalesAuditAdapter implements SalesPorts.AuditPort {
        private static final Logger LOGGER = LoggerFactory.getLogger(LoggingSalesAuditAdapter.class);

        @Override
        public void record(final String actor, final String action, final UUID targetId) {
            LOGGER.info("salesAudit actor={} action={} targetId={}", actor, action, targetId);
        }
    }

    @Component
    public static final class LoggingSalesEventAdapter implements SalesPorts.DomainEventPort {
        private static final Logger LOGGER = LoggerFactory.getLogger(LoggingSalesEventAdapter.class);

        @Override
        public void publish(final String eventType, final UUID aggregateId) {
            LOGGER.info("salesEvent type={} aggregateId={}", eventType, aggregateId);
        }
    }

    @Component
    public static final class ExistingSalesAttachmentAdapter implements SalesPorts.AttachmentPort {
        @Override
        public void attach(final UUID aggregateId, final UUID attachmentId) {
            requireId(aggregateId, "aggregate");
            requireId(attachmentId, "attachment");
        }
    }

    @Component
    public static final class CapabilityAuthorizationAdapter implements SalesPorts.AuthorizationPort {
        @Override
        public void requirePermission(final String actor, final String capability) {
            requireText(actor, "actor");
            requireText(capability, "capability");
        }

        @Override
        public void requireCustomerScope(final String actor, final UUID customerId) {
            requireText(actor, "actor");
            requireId(customerId, "customer");
        }
    }

    private static void requireQuantity(final SalesQuantity quantity) {
        if (quantity == null || !quantity.isPositive()) {
            throw new IllegalArgumentException("Sales inventory quantity must be positive.");
        }
    }

    private static void requireId(final UUID id, final String name) {
        if (id == null) {
            throw new IllegalArgumentException("Sales " + name + " reference is required.");
        }
    }

    private static void requireText(final String value, final String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Sales " + name + " is required.");
        }
    }

    private SalesInfrastructureAdapters() {
    }
}
