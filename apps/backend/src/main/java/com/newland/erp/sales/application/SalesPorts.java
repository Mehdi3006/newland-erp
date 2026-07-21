package com.newland.erp.sales.application;

import com.newland.erp.sales.domain.SalesQuantity;

import java.util.UUID;

public final class SalesPorts {
    public interface ProductCatalogPort {
        void requireSku(UUID productId, UUID skuId, String skuCode);
    }

    public interface MasterDataPort {
        void requireUom(String uomCode);

        void requireCurrency(UUID currencyId);

        void requireTaxCategory(UUID taxCategoryId);

        void requirePaymentTerms(UUID paymentTermsId);

        void requireShippingMethod(UUID shippingMethodId);

        void requireIncoterms(UUID incotermsId);
    }

    public interface EnterpriseScopePort {
        void requireCompanyBranchWarehouse(UUID companyId, UUID branchId, UUID warehouseId);

        void requireSalesChannel(UUID salesChannelId);
    }

    public interface InventoryPort {
        void checkAvailability(UUID skuId, UUID warehouseId, SalesQuantity quantity);

        void requestReservation(UUID salesOrderId, UUID lineId, SalesQuantity quantity);

        void requestDelivery(UUID salesOrderId, UUID lineId, SalesQuantity quantity);
    }

    public interface NumberSeriesPort {
        String nextNumber(String prefix);
    }

    public interface AuditPort {
        void record(String actor, String action, UUID targetId);
    }

    public interface DomainEventPort {
        void publish(String eventType, UUID aggregateId);
    }

    public interface AttachmentPort {
        void attach(UUID aggregateId, UUID attachmentId);
    }

    public interface AuthorizationPort {
        void requirePermission(String actor, String capability);

        void requireCustomerScope(String actor, UUID customerId);
    }

    private SalesPorts() {
    }
}
