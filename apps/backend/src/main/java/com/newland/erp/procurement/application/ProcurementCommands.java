package com.newland.erp.procurement.application;

import com.newland.erp.procurement.domain.ProcurementLine;
import com.newland.erp.procurement.domain.ProcurementQuantity;
import com.newland.erp.procurement.domain.PurchaseOrder;
import com.newland.erp.procurement.domain.Supplier;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ProcurementCommands {
    public record CreateSupplier(String idempotencyKey, String supplierCode, String name,
                                 List<Supplier.SupplierContact> contacts,
                                 List<Supplier.SupplierAddress> addresses,
                                 List<Supplier.SupplierProductReference> productReferences,
                                 List<UUID> attachmentIds, String actor) {
        public CreateSupplier {
            contacts = contacts == null ? List.of() : List.copyOf(contacts);
            addresses = addresses == null ? List.of() : List.copyOf(addresses);
            productReferences = productReferences == null ? List.of() : List.copyOf(productReferences);
            attachmentIds = attachmentIds == null ? List.of() : List.copyOf(attachmentIds);
        }
    }

    public record SubmitRequisition(String idempotencyKey, UUID companyId, UUID branchId, UUID warehouseId,
                                    List<ProcurementLine> lines, List<UUID> attachmentIds, String actor) {
        public SubmitRequisition {
            lines = lines == null ? List.of() : List.copyOf(lines);
            attachmentIds = attachmentIds == null ? List.of() : List.copyOf(attachmentIds);
        }
    }

    public record ApproveRequisition(UUID requisitionId, String actor) {
    }

    public record RejectRequisition(UUID requisitionId, String actor) {
    }

    public record ResubmitRequisition(UUID requisitionId, String idempotencyKey, String actor) {
    }

    public record CreateRfq(String idempotencyKey, UUID requisitionId, List<UUID> invitedSupplierIds,
                            String actor) {
        public CreateRfq {
            invitedSupplierIds = invitedSupplierIds == null ? List.of() : List.copyOf(invitedSupplierIds);
        }
    }

    public record SubmitQuotation(String idempotencyKey, UUID rfqId, UUID supplierId, UUID currencyId,
                                  UUID paymentTermsId, UUID shippingMethodId, UUID incotermsId,
                                  List<ProcurementLine> lines, List<UUID> attachmentIds, String actor) {
        public SubmitQuotation {
            lines = lines == null ? List.of() : List.copyOf(lines);
            attachmentIds = attachmentIds == null ? List.of() : List.copyOf(attachmentIds);
        }
    }

    public record CompareQuotations(UUID rfqId, UUID selectedQuotationId, List<UUID> quotationIds, String actor) {
        public CompareQuotations {
            quotationIds = quotationIds == null ? List.of() : List.copyOf(quotationIds);
        }
    }

    public record CreatePurchaseOrder(String idempotencyKey, UUID requisitionId, boolean directPurchase,
                                      UUID supplierId, UUID companyId, UUID branchId, UUID warehouseId,
                                      UUID currencyId, List<PurchaseOrder.PurchaseOrderLine> lines,
                                      LocalDate expectedDeliveryDate, List<UUID> attachmentIds, String actor) {
        public CreatePurchaseOrder {
            lines = lines == null ? List.of() : List.copyOf(lines);
            attachmentIds = attachmentIds == null ? List.of() : List.copyOf(attachmentIds);
        }
    }

    public record ApprovePurchaseOrder(UUID purchaseOrderId, String actor) {
    }

    public record RecordPartialDelivery(UUID purchaseOrderId, UUID lineId, ProcurementQuantity quantity,
                                        String actor) {
    }

    public record AmendPurchaseOrder(UUID purchaseOrderId, String reason,
                                     List<PurchaseOrder.PurchaseOrderLine> revisedLines, String actor) {
        public AmendPurchaseOrder {
            revisedLines = revisedLines == null ? List.of() : List.copyOf(revisedLines);
        }
    }

    public record CancelPurchaseOrder(UUID purchaseOrderId, String actor) {
    }

    private ProcurementCommands() {
    }
}
