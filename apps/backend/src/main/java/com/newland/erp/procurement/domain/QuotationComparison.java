package com.newland.erp.procurement.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuotationComparison(UUID id, UUID rfqId, UUID selectedQuotationId,
                                  List<UUID> comparedQuotationIds, Instant comparedAt, String actor) {
    public QuotationComparison {
        if (id == null || rfqId == null || selectedQuotationId == null || comparedAt == null) {
            throw new IllegalArgumentException("Quotation comparison identifiers are required.");
        }
        comparedQuotationIds = comparedQuotationIds == null ? List.of() : List.copyOf(comparedQuotationIds);
        if (!comparedQuotationIds.contains(selectedQuotationId)) {
            throw new ProcurementConflictException("Selected quotation must be included in comparison.");
        }
        actor = PurchaseRequisition.required("actor", actor);
    }
}
