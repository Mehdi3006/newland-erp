package com.newland.erp.procurement.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SupplierQuotation(UUID id, String quotationNumber, String idempotencyKey, UUID rfqId, UUID supplierId,
                                UUID currencyId, UUID paymentTermsId, UUID shippingMethodId, UUID incotermsId,
                                SupplierQuotationStatus status, List<ProcurementLine> lines, Instant submittedAt) {
    public SupplierQuotation {
        if (id == null || rfqId == null || supplierId == null || currencyId == null || status == null
                || submittedAt == null) {
            throw new IllegalArgumentException("Supplier quotation identifiers and commercial terms are required.");
        }
        quotationNumber = PurchaseRequisition.required("quotationNumber", quotationNumber).toUpperCase();
        idempotencyKey = PurchaseRequisition.required("idempotencyKey", idempotencyKey);
        lines = lines == null ? List.of() : List.copyOf(lines);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Supplier quotation requires at least one line.");
        }
    }
}
