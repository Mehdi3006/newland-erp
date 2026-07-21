package com.newland.erp.procurement.application;

import com.newland.erp.procurement.domain.ProcurementLine;
import com.newland.erp.procurement.domain.ProcurementQuantity;

import java.util.UUID;

public final class ProcurementPorts {
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
    }

    public interface InventoryReceiptPort {
        void requestReceipt(UUID purchaseOrderId, UUID lineId, ProcurementQuantity quantity);
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

        void requireSupplierScope(String actor, UUID supplierId);
    }

    public interface ProductLineValidator {
        default void validate(final ProcurementLine line, final ProductCatalogPort catalog,
                              final MasterDataPort masterData) {
            catalog.requireSku(line.productId(), line.skuId(), line.skuCode());
            masterData.requireUom(line.quantity().uomCode());
            if (line.taxCategoryId() != null) {
                masterData.requireTaxCategory(line.taxCategoryId());
            }
        }
    }

    private ProcurementPorts() {
    }
}
