package com.newland.erp.sales.application;

import com.newland.erp.sales.domain.Customer;
import com.newland.erp.sales.domain.CustomerStatus;
import com.newland.erp.sales.domain.SalesLine;
import com.newland.erp.sales.domain.SalesOrder;
import com.newland.erp.sales.domain.SalesQuantity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class SalesCommands {
    public record CreateCustomer(String idempotencyKey, String customerCode, String name,
                                 List<Customer.CustomerContact> contacts,
                                 List<Customer.CustomerAddress> addresses,
                                 List<Customer.CustomerCreditProfile> creditProfiles,
                                 List<Customer.CustomerProductReference> productReferences,
                                 List<UUID> attachmentIds, String actor) {
        public CreateCustomer {
            contacts = contacts == null ? List.of() : List.copyOf(contacts);
            addresses = addresses == null ? List.of() : List.copyOf(addresses);
            creditProfiles = creditProfiles == null ? List.of() : List.copyOf(creditProfiles);
            productReferences = productReferences == null ? List.of() : List.copyOf(productReferences);
            attachmentIds = attachmentIds == null ? List.of() : List.copyOf(attachmentIds);
        }
    }

    public record ChangeCustomerStatus(UUID customerId, CustomerStatus status, String actor) {
    }

    public record CreateQuotation(String idempotencyKey, UUID customerId, UUID companyId, UUID branchId,
                                  UUID warehouseId, UUID salesChannelId, UUID currencyId, UUID paymentTermsId,
                                  UUID shippingMethodId, UUID incotermsId, List<SalesLine> lines,
                                  LocalDate expiresOn, List<UUID> attachmentIds, String actor) {
        public CreateQuotation {
            lines = lines == null ? List.of() : List.copyOf(lines);
            attachmentIds = attachmentIds == null ? List.of() : List.copyOf(attachmentIds);
        }
    }

    public record ApproveQuotation(UUID quotationId, String actor) {
    }

    public record ExpireQuotation(UUID quotationId, LocalDate today, String actor) {
    }

    public record ReviseQuotation(UUID quotationId, String reason, List<SalesLine> revisedLines, String actor) {
        public ReviseQuotation {
            revisedLines = revisedLines == null ? List.of() : List.copyOf(revisedLines);
        }
    }

    public record CreateSalesOrder(String idempotencyKey, UUID quotationId, boolean directSales,
                                   boolean expiredQuotationOverride, UUID customerId, UUID companyId, UUID branchId,
                                   UUID warehouseId, UUID salesChannelId, UUID currencyId,
                                   List<SalesOrder.SalesOrderLine> lines, LocalDate requestedDeliveryDate,
                                   List<UUID> attachmentIds, String actor) {
        public CreateSalesOrder {
            lines = lines == null ? List.of() : List.copyOf(lines);
            attachmentIds = attachmentIds == null ? List.of() : List.copyOf(attachmentIds);
        }
    }

    public record ApproveSalesOrder(UUID salesOrderId, String actor) {
    }

    public record AmendSalesOrder(UUID salesOrderId, String reason,
                                  List<SalesOrder.SalesOrderLine> revisedLines, String actor) {
        public AmendSalesOrder {
            revisedLines = revisedLines == null ? List.of() : List.copyOf(revisedLines);
        }
    }

    public record ReserveInventory(UUID salesOrderId, UUID lineId, SalesQuantity quantity, String actor) {
    }

    public record TrackDelivery(UUID salesOrderId, UUID lineId, SalesQuantity quantity, String actor) {
    }

    public record CancelSalesOrder(UUID salesOrderId, String actor) {
    }

    private SalesCommands() {
    }
}
