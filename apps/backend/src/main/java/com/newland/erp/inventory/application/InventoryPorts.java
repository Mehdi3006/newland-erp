package com.newland.erp.inventory.application;

import com.newland.erp.inventory.domain.InventoryItemReference;

import java.util.UUID;

public final class InventoryPorts {
    public interface ProductCatalogPort {
        void requireSku(InventoryItemReference item);
    }

    public interface WarehouseReferencePort {
        void requireLocation(com.newland.erp.inventory.domain.InventoryLocation location);
    }

    public interface PlatformConfigurationPort {
        boolean negativeStockAllowed();
    }

    public interface NumberSeriesPort {
        String nextTransactionNumber(String prefix);
    }

    public interface AuditPort {
        void record(String actor, String action, UUID transactionId);
    }

    public interface DomainEventPort {
        void publish(String eventType, UUID aggregateId);
    }

    public interface AttachmentPort {
        void attach(UUID transactionId, UUID attachmentId);
    }

    public interface AuthorizationPort {
        void requirePermission(String actor, String capability);
    }

    private InventoryPorts() {
    }
}
