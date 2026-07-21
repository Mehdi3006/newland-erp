package com.newland.erp.procurement.api;

import com.newland.erp.procurement.domain.ProcurementLine;
import com.newland.erp.procurement.domain.ProcurementQuantity;
import com.newland.erp.procurement.domain.PurchaseOrder;
import com.newland.erp.procurement.domain.PurchaseRequisition;
import com.newland.erp.procurement.domain.RequestForQuotation;
import com.newland.erp.procurement.domain.Supplier;
import com.newland.erp.procurement.domain.SupplierQuotation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ProcurementDtos {
    public record QuantityRequest(@NotNull @Positive BigDecimal value, @NotBlank String uomCode) {
        ProcurementQuantity toDomain() {
            return new ProcurementQuantity(value, uomCode);
        }
    }

    public record LineRequest(@NotNull UUID productId, @NotNull UUID skuId, @NotBlank String skuCode,
                              @Valid @NotNull QuantityRequest quantity, BigDecimal unitPrice,
                              UUID taxCategoryId) {
        ProcurementLine toDomain() {
            return new ProcurementLine(UUID.randomUUID(), productId, skuId, skuCode, quantity.toDomain(), unitPrice,
                    taxCategoryId);
        }
    }

    public record PurchaseOrderLineRequest(@NotNull UUID productId, @NotNull UUID skuId,
                                           @NotBlank String skuCode,
                                           @Valid @NotNull QuantityRequest orderedQuantity,
                                           UUID taxCategoryId) {
        PurchaseOrder.PurchaseOrderLine toDomain() {
            final ProcurementQuantity zero = new ProcurementQuantity(BigDecimal.ZERO, orderedQuantity.uomCode());
            return new PurchaseOrder.PurchaseOrderLine(UUID.randomUUID(), productId, skuId, skuCode,
                    orderedQuantity.toDomain(), zero, zero, taxCategoryId);
        }
    }

    public record SupplierContactRequest(@NotBlank String name, String email, String phone) {
        Supplier.SupplierContact toDomain() {
            return new Supplier.SupplierContact(UUID.randomUUID(), name, email, phone);
        }
    }

    public record SupplierAddressRequest(@NotNull UUID countryId, UUID provinceId, UUID cityId,
                                         @NotBlank String addressLine) {
        Supplier.SupplierAddress toDomain() {
            return new Supplier.SupplierAddress(UUID.randomUUID(), countryId, provinceId, cityId, addressLine);
        }
    }

    public record SupplierProductReferenceRequest(@NotNull UUID productId, @NotNull UUID skuId,
                                                  @NotBlank String supplierSku, int leadTimeDays,
                                                  @Valid @NotNull QuantityRequest minimumOrderQuantity,
                                                  String packagingInformation) {
        Supplier.SupplierProductReference toDomain() {
            return new Supplier.SupplierProductReference(UUID.randomUUID(), productId, skuId, supplierSku,
                    leadTimeDays, minimumOrderQuantity.toDomain(), packagingInformation);
        }
    }

    public record CreateSupplierRequest(@NotBlank String idempotencyKey, @NotBlank String supplierCode,
                                        @NotBlank String name,
                                        List<@Valid SupplierContactRequest> contacts,
                                        List<@Valid SupplierAddressRequest> addresses,
                                        List<@Valid SupplierProductReferenceRequest> productReferences,
                                        List<UUID> attachmentIds) {
    }

    public record SubmitRequisitionRequest(@NotBlank String idempotencyKey, @NotNull UUID companyId,
                                           @NotNull UUID branchId, @NotNull UUID warehouseId,
                                           @NotEmpty List<@Valid LineRequest> lines,
                                           List<UUID> attachmentIds) {
    }

    public record ResubmitRequest(@NotBlank String idempotencyKey) {
    }

    public record CreateRfqRequest(@NotBlank String idempotencyKey, @NotNull UUID requisitionId,
                                   @NotEmpty List<UUID> invitedSupplierIds) {
    }

    public record SubmitQuotationRequest(@NotBlank String idempotencyKey, @NotNull UUID rfqId,
                                         @NotNull UUID supplierId, @NotNull UUID currencyId,
                                         UUID paymentTermsId, UUID shippingMethodId, UUID incotermsId,
                                         @NotEmpty List<@Valid LineRequest> lines, List<UUID> attachmentIds) {
    }

    public record CompareQuotationsRequest(@NotNull UUID rfqId, @NotNull UUID selectedQuotationId,
                                           @NotEmpty List<UUID> quotationIds) {
    }

    public record CreatePurchaseOrderRequest(@NotBlank String idempotencyKey, UUID requisitionId,
                                             boolean directPurchase, @NotNull UUID supplierId,
                                             @NotNull UUID companyId, @NotNull UUID branchId,
                                             @NotNull UUID warehouseId, @NotNull UUID currencyId,
                                             @NotEmpty List<@Valid PurchaseOrderLineRequest> lines,
                                             LocalDate expectedDeliveryDate, List<UUID> attachmentIds) {
    }

    public record DeliveryRequest(@NotNull UUID lineId, @Valid @NotNull QuantityRequest quantity) {
    }

    public record AmendPurchaseOrderRequest(@NotBlank String reason,
                                            @NotEmpty List<@Valid PurchaseOrderLineRequest> revisedLines) {
    }

    public record SupplierResponse(UUID id, String supplierCode, String name, String status) {
        static SupplierResponse from(final Supplier supplier) {
            return new SupplierResponse(supplier.id(), supplier.supplierCode(), supplier.name(),
                    supplier.status().name());
        }
    }

    public record RequisitionResponse(UUID id, String requisitionNumber, String status, int revision) {
        static RequisitionResponse from(final PurchaseRequisition requisition) {
            return new RequisitionResponse(requisition.id(), requisition.requisitionNumber(),
                    requisition.status().name(), requisition.revision());
        }
    }

    public record RfqResponse(UUID id, String rfqNumber, String status, List<UUID> invitedSupplierIds) {
        static RfqResponse from(final RequestForQuotation rfq) {
            return new RfqResponse(rfq.id(), rfq.rfqNumber(), rfq.status().name(), rfq.invitedSupplierIds());
        }
    }

    public record QuotationResponse(UUID id, String quotationNumber, String status) {
        static QuotationResponse from(final SupplierQuotation quotation) {
            return new QuotationResponse(quotation.id(), quotation.quotationNumber(), quotation.status().name());
        }
    }

    public record PurchaseOrderResponse(UUID id, String orderNumber, String status, int revision,
                                        BigDecimal totalRemainingQuantity) {
        static PurchaseOrderResponse from(final PurchaseOrder order) {
            final BigDecimal remaining = order.lines().stream()
                    .map(line -> line.remainingQuantity().value())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return new PurchaseOrderResponse(order.id(), order.orderNumber(), order.status().name(),
                    order.revision(), remaining);
        }
    }

    private ProcurementDtos() {
    }
}
