package com.newland.erp.productcatalog.infrastructure;

import com.newland.erp.productcatalog.application.ProductCatalogPorts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

public final class ProductCatalogInfrastructureAdapters {
    @Component
    public static final class LoggingAuditAdapter implements ProductCatalogPorts.AuditPort {
        private static final Logger LOGGER = LoggerFactory.getLogger(LoggingAuditAdapter.class);

        @Override
        public void record(final String actor, final String action, final UUID productId) {
            LOGGER.info("productCatalogAudit actor={} action={} productId={}", actor, action, productId);
        }
    }

    @Component
    public static final class PlatformAttachmentAdapter implements ProductCatalogPorts.AttachmentPort {
        @Override
        public void requireAttachment(final UUID attachmentId) {
            if (attachmentId == null) {
                throw new IllegalArgumentException("Attachment id is required.");
            }
        }
    }

    @Component
    public static final class PlatformLocalizationAdapter implements ProductCatalogPorts.LocalizationPort {
        @Override
        public void requireLanguage(final String languageCode) {
            if (languageCode == null || languageCode.isBlank()) {
                throw new IllegalArgumentException("Language code is required.");
            }
        }
    }

    private ProductCatalogInfrastructureAdapters() {
    }
}
