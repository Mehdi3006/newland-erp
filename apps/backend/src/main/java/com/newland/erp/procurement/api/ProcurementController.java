package com.newland.erp.procurement.api;

import com.newland.erp.procurement.application.ProcurementCommands;
import com.newland.erp.procurement.application.ProcurementService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/procurement")
public final class ProcurementController {
    private static final String ACTOR_HEADER = "X-Newland-Actor";
    private final ProcurementService service;

    public ProcurementController(final ProcurementService procurementService) {
        this.service = procurementService;
    }

    @PostMapping("/suppliers")
    @ResponseStatus(HttpStatus.CREATED)
    public ProcurementDtos.SupplierResponse createSupplier(@Valid @RequestBody
                                                           final ProcurementDtos.CreateSupplierRequest request,
                                                           @RequestHeader(name = ACTOR_HEADER,
                                                                   defaultValue = "system") final String actor) {
        return ProcurementDtos.SupplierResponse.from(service.createSupplier(new ProcurementCommands.CreateSupplier(
                request.idempotencyKey(), request.supplierCode(), request.name(),
                request.contacts() == null ? List.of()
                        : request.contacts().stream().map(ProcurementDtos.SupplierContactRequest::toDomain).toList(),
                request.addresses() == null ? List.of()
                        : request.addresses().stream().map(ProcurementDtos.SupplierAddressRequest::toDomain).toList(),
                request.productReferences() == null ? List.of() : request.productReferences().stream()
                        .map(ProcurementDtos.SupplierProductReferenceRequest::toDomain).toList(),
                request.attachmentIds(), actor)));
    }

    @GetMapping("/suppliers")
    public List<ProcurementDtos.SupplierResponse> suppliers() {
        return service.suppliers().stream().map(ProcurementDtos.SupplierResponse::from).toList();
    }

    @PostMapping("/requisitions")
    @ResponseStatus(HttpStatus.CREATED)
    public ProcurementDtos.RequisitionResponse submitRequisition(@Valid @RequestBody
            final ProcurementDtos.SubmitRequisitionRequest request,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor) {
        return ProcurementDtos.RequisitionResponse.from(service.submitRequisition(
                new ProcurementCommands.SubmitRequisition(request.idempotencyKey(), request.companyId(),
                        request.branchId(), request.warehouseId(),
                        request.lines().stream().map(ProcurementDtos.LineRequest::toDomain).toList(),
                        request.attachmentIds(), actor)));
    }

    @PostMapping("/requisitions/{requisitionId}/approve")
    public ProcurementDtos.RequisitionResponse approveRequisition(@PathVariable final UUID requisitionId,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor) {
        return ProcurementDtos.RequisitionResponse.from(service.approveRequisition(
                new ProcurementCommands.ApproveRequisition(requisitionId, actor)));
    }

    @PostMapping("/requisitions/{requisitionId}/reject")
    public ProcurementDtos.RequisitionResponse rejectRequisition(@PathVariable final UUID requisitionId,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor) {
        return ProcurementDtos.RequisitionResponse.from(service.rejectRequisition(
                new ProcurementCommands.RejectRequisition(requisitionId, actor)));
    }

    @PostMapping("/requisitions/{requisitionId}/resubmit")
    public ProcurementDtos.RequisitionResponse resubmitRequisition(@PathVariable final UUID requisitionId,
            @Valid @RequestBody final ProcurementDtos.ResubmitRequest request,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor) {
        return ProcurementDtos.RequisitionResponse.from(service.resubmitRequisition(
                new ProcurementCommands.ResubmitRequisition(requisitionId, request.idempotencyKey(), actor)));
    }

    @PostMapping("/rfqs")
    @ResponseStatus(HttpStatus.CREATED)
    public ProcurementDtos.RfqResponse createRfq(@Valid @RequestBody final ProcurementDtos.CreateRfqRequest request,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor) {
        return ProcurementDtos.RfqResponse.from(service.createRfq(new ProcurementCommands.CreateRfq(
                request.idempotencyKey(), request.requisitionId(), request.invitedSupplierIds(), actor)));
    }

    @PostMapping("/quotations")
    @ResponseStatus(HttpStatus.CREATED)
    public ProcurementDtos.QuotationResponse submitQuotation(@Valid @RequestBody
            final ProcurementDtos.SubmitQuotationRequest request,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor) {
        return ProcurementDtos.QuotationResponse.from(service.submitQuotation(new ProcurementCommands.SubmitQuotation(
                request.idempotencyKey(), request.rfqId(), request.supplierId(), request.currencyId(),
                request.paymentTermsId(), request.shippingMethodId(), request.incotermsId(),
                request.lines().stream().map(ProcurementDtos.LineRequest::toDomain).toList(),
                request.attachmentIds(), actor)));
    }

    @PostMapping("/quotation-comparisons")
    @ResponseStatus(HttpStatus.CREATED)
    public void compareQuotations(@Valid @RequestBody final ProcurementDtos.CompareQuotationsRequest request,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor) {
        service.compareQuotations(new ProcurementCommands.CompareQuotations(request.rfqId(),
                request.selectedQuotationId(), request.quotationIds(), actor));
    }

    @PostMapping("/purchase-orders")
    @ResponseStatus(HttpStatus.CREATED)
    public ProcurementDtos.PurchaseOrderResponse createPurchaseOrder(@Valid @RequestBody
            final ProcurementDtos.CreatePurchaseOrderRequest request,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor) {
        return ProcurementDtos.PurchaseOrderResponse.from(service.createPurchaseOrder(
                new ProcurementCommands.CreatePurchaseOrder(request.idempotencyKey(), request.requisitionId(),
                        request.directPurchase(), request.supplierId(), request.companyId(), request.branchId(),
                        request.warehouseId(), request.currencyId(),
                        request.lines().stream().map(ProcurementDtos.PurchaseOrderLineRequest::toDomain).toList(),
                        request.expectedDeliveryDate(), request.attachmentIds(), actor)));
    }

    @PostMapping("/purchase-orders/{purchaseOrderId}/approve")
    public ProcurementDtos.PurchaseOrderResponse approvePurchaseOrder(@PathVariable final UUID purchaseOrderId,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor) {
        return ProcurementDtos.PurchaseOrderResponse.from(service.approvePurchaseOrder(
                new ProcurementCommands.ApprovePurchaseOrder(purchaseOrderId, actor)));
    }

    @PostMapping("/purchase-orders/{purchaseOrderId}/deliveries")
    public ProcurementDtos.PurchaseOrderResponse recordPartialDelivery(@PathVariable final UUID purchaseOrderId,
            @Valid @RequestBody final ProcurementDtos.DeliveryRequest request,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor) {
        return ProcurementDtos.PurchaseOrderResponse.from(service.recordPartialDelivery(
                new ProcurementCommands.RecordPartialDelivery(purchaseOrderId, request.lineId(),
                        request.quantity().toDomain(), actor)));
    }

    @PostMapping("/purchase-orders/{purchaseOrderId}/amend")
    public ProcurementDtos.PurchaseOrderResponse amendPurchaseOrder(@PathVariable final UUID purchaseOrderId,
            @Valid @RequestBody final ProcurementDtos.AmendPurchaseOrderRequest request,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor) {
        return ProcurementDtos.PurchaseOrderResponse.from(service.amendPurchaseOrder(
                new ProcurementCommands.AmendPurchaseOrder(purchaseOrderId, request.reason(),
                        request.revisedLines().stream()
                                .map(ProcurementDtos.PurchaseOrderLineRequest::toDomain).toList(), actor)));
    }

    @PostMapping("/purchase-orders/{purchaseOrderId}/cancel")
    public ProcurementDtos.PurchaseOrderResponse cancelPurchaseOrder(@PathVariable final UUID purchaseOrderId,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor) {
        return ProcurementDtos.PurchaseOrderResponse.from(service.cancelPurchaseOrder(
                new ProcurementCommands.CancelPurchaseOrder(purchaseOrderId, actor)));
    }
}
