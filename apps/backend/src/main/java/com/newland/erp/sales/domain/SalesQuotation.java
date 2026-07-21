package com.newland.erp.sales.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SalesQuotation(UUID id, String quotationNumber, String idempotencyKey, UUID customerId,
                             UUID companyId, UUID branchId, UUID warehouseId, UUID salesChannelId,
                             UUID currencyId, UUID paymentTermsId, UUID shippingMethodId, UUID incotermsId,
                             SalesQuotationStatus status, int revision, List<SalesLine> lines,
                             LocalDate expiresOn, Instant createdAt, String actor) {
    public SalesQuotation {
        if (id == null || customerId == null || companyId == null || branchId == null || warehouseId == null
                || salesChannelId == null || currencyId == null || status == null || createdAt == null) {
            throw new IllegalArgumentException("Sales quotation identifiers and commercial terms are required.");
        }
        quotationNumber = SalesLine.required("quotationNumber", quotationNumber).toUpperCase();
        idempotencyKey = SalesLine.required("idempotencyKey", idempotencyKey);
        actor = SalesLine.required("actor", actor);
        lines = lines == null ? List.of() : List.copyOf(lines);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Sales quotation requires at least one line.");
        }
        if (revision < 0) {
            throw new IllegalArgumentException("Sales quotation revision cannot be negative.");
        }
    }

    public SalesQuotation submit() {
        if (status != SalesQuotationStatus.DRAFT) {
            throw new SalesConflictException("Only draft quotations can be submitted.");
        }
        return withStatus(SalesQuotationStatus.SUBMITTED);
    }

    public SalesQuotation approve() {
        if (status != SalesQuotationStatus.SUBMITTED) {
            throw new SalesConflictException("Only submitted quotations can be approved.");
        }
        return withStatus(SalesQuotationStatus.APPROVED);
    }

    public SalesQuotation expire(final LocalDate today) {
        if (expiresOn == null || !expiresOn.isBefore(today)) {
            throw new SalesConflictException("Quotation is not expired.");
        }
        return withStatus(SalesQuotationStatus.EXPIRED);
    }

    public SalesQuotation revise(final List<SalesLine> revisedLines) {
        if (status != SalesQuotationStatus.APPROVED && status != SalesQuotationStatus.EXPIRED) {
            throw new SalesConflictException("Only approved or expired quotations require controlled revision.");
        }
        return new SalesQuotation(id, quotationNumber, idempotencyKey, customerId, companyId, branchId,
                warehouseId, salesChannelId, currencyId, paymentTermsId, shippingMethodId, incotermsId,
                SalesQuotationStatus.DRAFT, revision + 1, revisedLines, expiresOn, createdAt, actor);
    }

    public SalesQuotation converted() {
        if (status != SalesQuotationStatus.APPROVED) {
            throw new SalesConflictException("Only approved quotations can be converted.");
        }
        return withStatus(SalesQuotationStatus.CONVERTED);
    }

    private SalesQuotation withStatus(final SalesQuotationStatus nextStatus) {
        return new SalesQuotation(id, quotationNumber, idempotencyKey, customerId, companyId, branchId,
                warehouseId, salesChannelId, currencyId, paymentTermsId, shippingMethodId, incotermsId, nextStatus,
                revision, lines, expiresOn, createdAt, actor);
    }
}
