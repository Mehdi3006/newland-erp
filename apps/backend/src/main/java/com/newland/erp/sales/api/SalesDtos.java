package com.newland.erp.sales.api;

import com.newland.erp.sales.domain.Customer;
import com.newland.erp.sales.domain.CustomerStatus;
import com.newland.erp.sales.domain.SalesLine;
import com.newland.erp.sales.domain.SalesOrder;
import com.newland.erp.sales.domain.SalesQuantity;
import com.newland.erp.sales.domain.SalesQuotation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class SalesDtos {
    public record QuantityRequest(@NotNull @Positive BigDecimal value, @NotBlank String uomCode) {
        SalesQuantity toDomain() {
            return new SalesQuantity(value, uomCode);
        }
    }

    public record LineRequest(@NotNull UUID productId, @NotNull UUID skuId, @NotBlank String skuCode,
                              @Valid @NotNull QuantityRequest quantity, BigDecimal unitPrice,
                              UUID taxCategoryId) {
        SalesLine toDomain() {
            return new SalesLine(UUID.randomUUID(), productId, skuId, skuCode, quantity.toDomain(), unitPrice,
                    taxCategoryId);
        }
    }

    public record OrderLineRequest(@NotNull UUID productId, @NotNull UUID skuId, @NotBlank String skuCode,
                                   @Valid @NotNull QuantityRequest orderedQuantity, UUID taxCategoryId) {
        SalesOrder.SalesOrderLine toDomain() {
            final SalesQuantity zero = new SalesQuantity(BigDecimal.ZERO, orderedQuantity.uomCode());
            return new SalesOrder.SalesOrderLine(UUID.randomUUID(), productId, skuId, skuCode,
                    orderedQuantity.toDomain(), zero, zero, zero, taxCategoryId, null);
        }
    }

    public record ContactRequest(@NotBlank String name, String email, String phone) {
        Customer.CustomerContact toDomain() {
            return new Customer.CustomerContact(UUID.randomUUID(), name, email, phone);
        }
    }

    public record AddressRequest(@NotNull UUID countryId, UUID provinceId, UUID cityId,
                                 @NotBlank String addressLine) {
        Customer.CustomerAddress toDomain() {
            return new Customer.CustomerAddress(UUID.randomUUID(), countryId, provinceId, cityId, addressLine);
        }
    }

    public record CreditProfileRequest(@NotNull UUID companyId, @NotNull UUID currencyId,
                                       @NotNull BigDecimal creditLimit, boolean creditHold) {
        Customer.CustomerCreditProfile toDomain() {
            return new Customer.CustomerCreditProfile(UUID.randomUUID(), companyId, currencyId, creditLimit,
                    creditHold);
        }
    }

    public record ProductReferenceRequest(@NotNull UUID productId, @NotNull UUID skuId,
                                          @NotBlank String customerSku) {
        Customer.CustomerProductReference toDomain() {
            return new Customer.CustomerProductReference(UUID.randomUUID(), productId, skuId, customerSku);
        }
    }

    public record CreateCustomerRequest(@NotBlank String idempotencyKey, @NotBlank String customerCode,
                                        @NotBlank String name, List<@Valid ContactRequest> contacts,
                                        List<@Valid AddressRequest> addresses,
                                        List<@Valid CreditProfileRequest> creditProfiles,
                                        List<@Valid ProductReferenceRequest> productReferences,
                                        List<UUID> attachmentIds) {
    }

    public record ChangeCustomerStatusRequest(@NotNull CustomerStatus status) {
    }

    public record CreateQuotationRequest(@NotBlank String idempotencyKey, @NotNull UUID customerId,
                                         @NotNull UUID companyId, @NotNull UUID branchId,
                                         @NotNull UUID warehouseId, @NotNull UUID salesChannelId,
                                         @NotNull UUID currencyId, UUID paymentTermsId, UUID shippingMethodId,
                                         UUID incotermsId, @NotEmpty List<@Valid LineRequest> lines,
                                         LocalDate expiresOn, List<UUID> attachmentIds) {
    }

    public record ReviseQuotationRequest(@NotBlank String reason, @NotEmpty List<@Valid LineRequest> revisedLines) {
    }

    public record CreateSalesOrderRequest(@NotBlank String idempotencyKey, UUID quotationId, boolean directSales,
                                          boolean expiredQuotationOverride, @NotNull UUID customerId,
                                          @NotNull UUID companyId, @NotNull UUID branchId,
                                          @NotNull UUID warehouseId, @NotNull UUID salesChannelId,
                                          @NotNull UUID currencyId,
                                          @NotEmpty List<@Valid OrderLineRequest> lines,
                                          LocalDate requestedDeliveryDate, List<UUID> attachmentIds) {
    }

    public record AmendOrderRequest(@NotBlank String reason, @NotEmpty List<@Valid OrderLineRequest> revisedLines) {
    }

    public record QuantityCommandRequest(@NotNull UUID lineId, @Valid @NotNull QuantityRequest quantity) {
    }

    public record CustomerResponse(UUID id, String customerCode, String name, String status) {
        static CustomerResponse from(final Customer customer) {
            return new CustomerResponse(customer.id(), customer.customerCode(), customer.name(),
                    customer.status().name());
        }
    }

    public record QuotationResponse(UUID id, String quotationNumber, String status, int revision) {
        static QuotationResponse from(final SalesQuotation quotation) {
            return new QuotationResponse(quotation.id(), quotation.quotationNumber(), quotation.status().name(),
                    quotation.revision());
        }
    }

    public record OrderResponse(UUID id, String orderNumber, String status, int revision,
                                BigDecimal remainingQuantity) {
        static OrderResponse from(final SalesOrder order) {
            final BigDecimal remaining = order.lines().stream().map(line -> line.remainingQuantity().value())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return new OrderResponse(order.id(), order.orderNumber(), order.status().name(), order.revision(),
                    remaining);
        }
    }

    private SalesDtos() {
    }
}
