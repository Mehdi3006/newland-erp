package com.newland.erp.productcatalog.application;

import java.util.UUID;

public final class ProductCatalogPorts {
    public interface AuditPort {
        void record(String actor, String action, UUID productId);
    }

    public interface AttachmentPort {
        void requireAttachment(UUID attachmentId);
    }

    public interface LocalizationPort {
        void requireLanguage(String languageCode);
    }

    private ProductCatalogPorts() {
    }
}
