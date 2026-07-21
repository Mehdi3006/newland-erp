package com.newland.erp.inventory.infrastructure;

import com.newland.erp.inventory.application.InventoryPorts;
import com.newland.erp.inventory.domain.InventoryItemReference;
import com.newland.erp.inventory.domain.InventoryLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public final class InventoryInfrastructureAdapters {
    @Component
    public static final class ExistingProductCatalogAdapter implements InventoryPorts.ProductCatalogPort {
        @Override
        public void requireSku(final InventoryItemReference item) {
            if (item == null) {
                throw new IllegalArgumentException("Inventory SKU reference is required.");
            }
        }
    }

    @Component
    public static final class ExistingWarehouseReferenceAdapter implements InventoryPorts.WarehouseReferencePort {
        @Override
        public void requireLocation(final InventoryLocation location) {
            if (location == null || location.warehouseId() == null) {
                throw new IllegalArgumentException("Inventory warehouse reference is required.");
            }
        }
    }

    @Component
    public static final class PlatformConfigurationAdapter implements InventoryPorts.PlatformConfigurationPort {
        @Override
        public boolean negativeStockAllowed() {
            return false;
        }
    }

    @Component
    public static final class SimpleNumberSeriesAdapter implements InventoryPorts.NumberSeriesPort {
        private final AtomicLong sequence = new AtomicLong();

        @Override
        public String nextTransactionNumber(final String prefix) {
            return prefix + "-" + sequence.incrementAndGet();
        }
    }

    @Component
    public static final class LoggingInventoryAuditAdapter implements InventoryPorts.AuditPort {
        private static final Logger LOGGER = LoggerFactory.getLogger(LoggingInventoryAuditAdapter.class);

        @Override
        public void record(final String actor, final String action, final UUID transactionId) {
            LOGGER.info("inventoryAudit actor={} action={} id={}", actor, action, transactionId);
        }
    }

    @Component
    public static final class LoggingInventoryEventAdapter implements InventoryPorts.DomainEventPort {
        private static final Logger LOGGER = LoggerFactory.getLogger(LoggingInventoryEventAdapter.class);

        @Override
        public void publish(final String eventType, final UUID aggregateId) {
            LOGGER.info("inventoryEvent type={} aggregateId={}", eventType, aggregateId);
        }
    }

    @Component
    public static final class ExistingAttachmentAdapter implements InventoryPorts.AttachmentPort {
        @Override
        public void attach(final UUID transactionId, final UUID attachmentId) {
            if (transactionId == null || attachmentId == null) {
                throw new IllegalArgumentException("Inventory attachment references are required.");
            }
        }
    }

    @Component
    public static final class HeaderAuthorizationAdapter implements InventoryPorts.AuthorizationPort {
        @Override
        public void requirePermission(final String actor, final String capability) {
            if (actor == null || actor.isBlank() || capability == null || capability.isBlank()) {
                throw new IllegalArgumentException("Inventory authorization context is required.");
            }
        }
    }

    private InventoryInfrastructureAdapters() {
    }
}
