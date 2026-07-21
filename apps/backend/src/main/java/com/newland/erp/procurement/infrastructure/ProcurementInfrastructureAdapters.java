package com.newland.erp.procurement.infrastructure;

import com.newland.erp.procurement.application.ProcurementPorts;
import com.newland.erp.procurement.domain.ProcurementQuantity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public final class ProcurementInfrastructureAdapters {
    @Component
    public static final class ExistingProductCatalogAdapter implements ProcurementPorts.ProductCatalogPort {
        @Override
        public void requireSku(final UUID productId, final UUID skuId, final String skuCode) {
            if (productId == null || skuId == null || skuCode == null || skuCode.isBlank()) {
                throw new IllegalArgumentException("Procurement product/SKU reference is required.");
            }
        }
    }

    @Component
    public static final class ExistingMasterDataAdapter implements ProcurementPorts.MasterDataPort {
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
    public static final class ExistingEnterpriseScopeAdapter implements ProcurementPorts.EnterpriseScopePort {
        @Override
        public void requireCompanyBranchWarehouse(final UUID companyId, final UUID branchId, final UUID warehouseId) {
            requireId(companyId, "company");
            requireId(branchId, "branch");
            requireId(warehouseId, "warehouse");
        }
    }

    @Component
    public static final class InventoryReceiptRequestAdapter implements ProcurementPorts.InventoryReceiptPort {
        private static final Logger LOGGER = LoggerFactory.getLogger(InventoryReceiptRequestAdapter.class);

        @Override
        public void requestReceipt(final UUID purchaseOrderId, final UUID lineId, final ProcurementQuantity quantity) {
            if (purchaseOrderId == null || lineId == null || quantity == null || !quantity.isPositive()) {
                throw new IllegalArgumentException("Procurement inventory receipt request is invalid.");
            }
            LOGGER.info("procurementInventoryReceiptRequested purchaseOrderId={} lineId={}", purchaseOrderId, lineId);
        }
    }

    @Component
    public static final class SimpleProcurementNumberSeriesAdapter implements ProcurementPorts.NumberSeriesPort {
        private final AtomicLong sequence = new AtomicLong();

        @Override
        public String nextNumber(final String prefix) {
            requireText(prefix, "number prefix");
            return prefix + "-" + sequence.incrementAndGet();
        }
    }

    @Component
    public static final class LoggingProcurementAuditAdapter implements ProcurementPorts.AuditPort {
        private static final Logger LOGGER = LoggerFactory.getLogger(LoggingProcurementAuditAdapter.class);

        @Override
        public void record(final String actor, final String action, final UUID targetId) {
            LOGGER.info("procurementAudit actor={} action={} targetId={}", actor, action, targetId);
        }
    }

    @Component
    public static final class LoggingProcurementEventAdapter implements ProcurementPorts.DomainEventPort {
        private static final Logger LOGGER = LoggerFactory.getLogger(LoggingProcurementEventAdapter.class);

        @Override
        public void publish(final String eventType, final UUID aggregateId) {
            LOGGER.info("procurementEvent type={} aggregateId={}", eventType, aggregateId);
        }
    }

    @Component
    public static final class ExistingProcurementAttachmentAdapter implements ProcurementPorts.AttachmentPort {
        @Override
        public void attach(final UUID aggregateId, final UUID attachmentId) {
            requireId(aggregateId, "aggregate");
            requireId(attachmentId, "attachment");
        }
    }

    @Component
    public static final class CapabilityAuthorizationAdapter implements ProcurementPorts.AuthorizationPort {
        @Override
        public void requirePermission(final String actor, final String capability) {
            requireText(actor, "actor");
            requireText(capability, "capability");
        }

        @Override
        public void requireSupplierScope(final String actor, final UUID supplierId) {
            requireText(actor, "actor");
            requireId(supplierId, "supplier");
        }
    }

    private static void requireId(final UUID id, final String name) {
        if (id == null) {
            throw new IllegalArgumentException("Procurement " + name + " reference is required.");
        }
    }

    private static void requireText(final String value, final String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Procurement " + name + " is required.");
        }
    }

    private ProcurementInfrastructureAdapters() {
    }
}
