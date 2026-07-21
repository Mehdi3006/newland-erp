package com.newland.erp.sales.api;

import com.newland.erp.sales.application.SalesCommands;
import com.newland.erp.sales.application.SalesService;

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
@RequestMapping("/api/v1/sales")
public final class SalesController {
    private static final String ACTOR_HEADER = "X-Newland-Actor";
    private final SalesService service;

    public SalesController(final SalesService salesService) {
        this.service = salesService;
    }

    @PostMapping("/customers")
    @ResponseStatus(HttpStatus.CREATED)
    public SalesDtos.CustomerResponse createCustomer(@Valid @RequestBody
                                                     final SalesDtos.CreateCustomerRequest request,
                                                     @RequestHeader(name = ACTOR_HEADER,
                                                             defaultValue = "system") final String actor) {
        return SalesDtos.CustomerResponse.from(service.createCustomer(new SalesCommands.CreateCustomer(
                request.idempotencyKey(), request.customerCode(), request.name(),
                request.contacts() == null ? List.of()
                        : request.contacts().stream().map(SalesDtos.ContactRequest::toDomain).toList(),
                request.addresses() == null ? List.of()
                        : request.addresses().stream().map(SalesDtos.AddressRequest::toDomain).toList(),
                request.creditProfiles() == null ? List.of()
                        : request.creditProfiles().stream().map(SalesDtos.CreditProfileRequest::toDomain).toList(),
                request.productReferences() == null ? List.of() : request.productReferences().stream()
                        .map(SalesDtos.ProductReferenceRequest::toDomain).toList(),
                request.attachmentIds(), actor)));
    }

    @GetMapping("/customers")
    public List<SalesDtos.CustomerResponse> customers() {
        return service.customers().stream().map(SalesDtos.CustomerResponse::from).toList();
    }

    @PostMapping("/customers/{customerId}/status")
    public SalesDtos.CustomerResponse changeCustomerStatus(@PathVariable final UUID customerId,
            @Valid @RequestBody final SalesDtos.ChangeCustomerStatusRequest request,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor) {
        return SalesDtos.CustomerResponse.from(service.changeCustomerStatus(new SalesCommands.ChangeCustomerStatus(
                customerId, request.status(), actor)));
    }

    @PostMapping("/quotations")
    @ResponseStatus(HttpStatus.CREATED)
    public SalesDtos.QuotationResponse createQuotation(@Valid @RequestBody
                                                       final SalesDtos.CreateQuotationRequest request,
                                                       @RequestHeader(name = ACTOR_HEADER,
                                                               defaultValue = "system") final String actor) {
        return SalesDtos.QuotationResponse.from(service.createQuotation(new SalesCommands.CreateQuotation(
                request.idempotencyKey(), request.customerId(), request.companyId(), request.branchId(),
                request.warehouseId(), request.salesChannelId(), request.currencyId(), request.paymentTermsId(),
                request.shippingMethodId(), request.incotermsId(),
                request.lines().stream().map(SalesDtos.LineRequest::toDomain).toList(), request.expiresOn(),
                request.attachmentIds(), actor)));
    }

    @PostMapping("/quotations/{quotationId}/approve")
    public SalesDtos.QuotationResponse approveQuotation(@PathVariable final UUID quotationId,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor) {
        return SalesDtos.QuotationResponse.from(service.approveQuotation(new SalesCommands.ApproveQuotation(
                quotationId, actor)));
    }

    @PostMapping("/quotations/{quotationId}/revise")
    public SalesDtos.QuotationResponse reviseQuotation(@PathVariable final UUID quotationId,
            @Valid @RequestBody final SalesDtos.ReviseQuotationRequest request,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor) {
        return SalesDtos.QuotationResponse.from(service.reviseQuotation(new SalesCommands.ReviseQuotation(
                quotationId, request.reason(), request.revisedLines().stream().map(SalesDtos.LineRequest::toDomain)
                .toList(), actor)));
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public SalesDtos.OrderResponse createSalesOrder(@Valid @RequestBody
                                                    final SalesDtos.CreateSalesOrderRequest request,
                                                    @RequestHeader(name = ACTOR_HEADER,
                                                            defaultValue = "system") final String actor) {
        return SalesDtos.OrderResponse.from(service.createSalesOrder(new SalesCommands.CreateSalesOrder(
                request.idempotencyKey(), request.quotationId(), request.directSales(),
                request.expiredQuotationOverride(), request.customerId(), request.companyId(), request.branchId(),
                request.warehouseId(), request.salesChannelId(), request.currencyId(),
                request.lines().stream().map(SalesDtos.OrderLineRequest::toDomain).toList(),
                request.requestedDeliveryDate(), request.attachmentIds(), actor)));
    }

    @PostMapping("/orders/{orderId}/approve")
    public SalesDtos.OrderResponse approveSalesOrder(@PathVariable final UUID orderId,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor) {
        return SalesDtos.OrderResponse.from(service.approveSalesOrder(new SalesCommands.ApproveSalesOrder(
                orderId, actor)));
    }

    @PostMapping("/orders/{orderId}/amend")
    public SalesDtos.OrderResponse amendSalesOrder(@PathVariable final UUID orderId,
            @Valid @RequestBody final SalesDtos.AmendOrderRequest request,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor) {
        return SalesDtos.OrderResponse.from(service.amendSalesOrder(new SalesCommands.AmendSalesOrder(orderId,
                request.reason(), request.revisedLines().stream().map(SalesDtos.OrderLineRequest::toDomain)
                .toList(), actor)));
    }

    @PostMapping("/orders/{orderId}/reservations")
    public SalesDtos.OrderResponse reserveInventory(@PathVariable final UUID orderId,
            @Valid @RequestBody final SalesDtos.QuantityCommandRequest request,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor) {
        return SalesDtos.OrderResponse.from(service.reserveInventory(new SalesCommands.ReserveInventory(orderId,
                request.lineId(), request.quantity().toDomain(), actor)));
    }

    @PostMapping("/orders/{orderId}/deliveries")
    public SalesDtos.OrderResponse trackDelivery(@PathVariable final UUID orderId,
            @Valid @RequestBody final SalesDtos.QuantityCommandRequest request,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor) {
        return SalesDtos.OrderResponse.from(service.trackDelivery(new SalesCommands.TrackDelivery(orderId,
                request.lineId(), request.quantity().toDomain(), actor)));
    }

    @PostMapping("/orders/{orderId}/cancel")
    public SalesDtos.OrderResponse cancelSalesOrder(@PathVariable final UUID orderId,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor) {
        return SalesDtos.OrderResponse.from(service.cancelSalesOrder(new SalesCommands.CancelSalesOrder(
                orderId, actor)));
    }
}
