package com.newland.erp.sales.application;

import com.newland.erp.sales.domain.Customer;
import com.newland.erp.sales.domain.CustomerStatus;
import com.newland.erp.sales.domain.SalesConflictException;
import com.newland.erp.sales.domain.SalesLine;
import com.newland.erp.sales.domain.SalesNotFoundException;
import com.newland.erp.sales.domain.SalesOrder;
import com.newland.erp.sales.domain.SalesOrderRevision;
import com.newland.erp.sales.domain.SalesOrderStatus;
import com.newland.erp.sales.domain.SalesQuotation;
import com.newland.erp.sales.domain.SalesQuotationRevision;
import com.newland.erp.sales.domain.SalesQuotationStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public final class SalesService {
    private final SalesRepository repository;
    private final SalesPorts.ProductCatalogPort catalog;
    private final SalesPorts.MasterDataPort masterData;
    private final SalesPorts.EnterpriseScopePort enterprise;
    private final SalesPorts.InventoryPort inventory;
    private final SalesPorts.NumberSeriesPort numbers;
    private final SalesPorts.AuditPort audit;
    private final SalesPorts.DomainEventPort events;
    private final SalesPorts.AttachmentPort attachments;
    private final SalesPorts.AuthorizationPort authorization;
    private final Clock clock;

    public SalesService(final SalesRepository salesRepository, final SalesPorts.ProductCatalogPort catalogPort,
                        final SalesPorts.MasterDataPort masterDataPort,
                        final SalesPorts.EnterpriseScopePort enterpriseScopePort,
                        final SalesPorts.InventoryPort inventoryPort, final SalesPorts.NumberSeriesPort numberPort,
                        final SalesPorts.AuditPort auditPort, final SalesPorts.DomainEventPort eventPort,
                        final SalesPorts.AttachmentPort attachmentPort,
                        final SalesPorts.AuthorizationPort authorizationPort, final Clock systemClock) {
        this.repository = salesRepository;
        this.catalog = catalogPort;
        this.masterData = masterDataPort;
        this.enterprise = enterpriseScopePort;
        this.inventory = inventoryPort;
        this.numbers = numberPort;
        this.audit = auditPort;
        this.events = eventPort;
        this.attachments = attachmentPort;
        this.authorization = authorizationPort;
        this.clock = systemClock;
    }

    @Transactional
    public Customer createCustomer(final SalesCommands.CreateCustomer command) {
        authorization.requirePermission(command.actor(), "sales.customer.create");
        assertIdempotent(command.idempotencyKey());
        if (repository.customerCodeExists(command.customerCode().trim().toUpperCase())) {
            throw new SalesConflictException("Duplicate customer code: " + command.customerCode());
        }
        command.creditProfiles().forEach(profile -> masterData.requireCurrency(profile.currencyId()));
        command.productReferences().forEach(reference -> catalog.requireSku(reference.productId(),
                reference.skuId(), reference.customerSku()));
        final Customer customer = repository.insertCustomer(new Customer(UUID.randomUUID(), command.idempotencyKey(),
                command.customerCode(), command.name(), CustomerStatus.ACTIVE, command.contacts(),
                command.addresses(), command.creditProfiles(), command.productReferences(), now()));
        command.attachmentIds().forEach(attachmentId -> attachments.attach(customer.id(), attachmentId));
        audit.record(command.actor(), "SALES_CUSTOMER_CREATED", customer.id());
        events.publish("SalesCustomerCreated", customer.id());
        return customer;
    }

    @Transactional
    public Customer changeCustomerStatus(final SalesCommands.ChangeCustomerStatus command) {
        authorization.requirePermission(command.actor(), "sales.customer.status.change");
        final Customer customer = customer(command.customerId()).transitionTo(command.status());
        repository.insertCustomer(customer);
        audit.record(command.actor(), "SALES_CUSTOMER_STATUS_CHANGED", customer.id());
        return customer;
    }

    @Transactional
    public SalesQuotation createQuotation(final SalesCommands.CreateQuotation command) {
        authorization.requirePermission(command.actor(), "sales.quotation.create");
        assertIdempotent(command.idempotencyKey());
        customer(command.customerId());
        authorization.requireCustomerScope(command.actor(), command.customerId());
        validateScope(command.companyId(), command.branchId(), command.warehouseId(), command.salesChannelId());
        validateCommercialTerms(command.currencyId(), command.paymentTermsId(), command.shippingMethodId(),
                command.incotermsId());
        command.lines().forEach(this::validateLine);
        final SalesQuotation quotation = repository.insertQuotation(new SalesQuotation(UUID.randomUUID(),
                numbers.nextNumber("SQ"), command.idempotencyKey(), command.customerId(), command.companyId(),
                command.branchId(), command.warehouseId(), command.salesChannelId(), command.currencyId(),
                command.paymentTermsId(), command.shippingMethodId(), command.incotermsId(),
                SalesQuotationStatus.SUBMITTED, 0, command.lines(), 0, command.expiresOn(), now(), command.actor()));
        command.attachmentIds().forEach(attachmentId -> attachments.attach(quotation.id(), attachmentId));
        audit.record(command.actor(), "SALES_QUOTATION_SUBMITTED", quotation.id());
        events.publish("SalesQuotationSubmitted", quotation.id());
        return quotation;
    }

    @Transactional
    public SalesQuotation approveQuotation(final SalesCommands.ApproveQuotation command) {
        authorization.requirePermission(command.actor(), "sales.quotation.approve");
        final SalesQuotation quotation = quotation(command.quotationId()).approve();
        repository.updateQuotation(quotation);
        audit.record(command.actor(), "SALES_QUOTATION_APPROVED", quotation.id());
        return quotation;
    }

    @Transactional
    public SalesQuotation rejectQuotation(final SalesCommands.RejectQuotation command) {
        authorization.requirePermission(command.actor(), "sales.quotation.reject");
        final SalesQuotation quotation = quotation(command.quotationId()).reject();
        repository.updateQuotation(quotation);
        audit.record(command.actor(), "SALES_QUOTATION_REJECTED", quotation.id());
        return quotation;
    }

    @Transactional
    public SalesQuotation expireQuotation(final SalesCommands.ExpireQuotation command) {
        authorization.requirePermission(command.actor(), "sales.quotation.expire");
        final SalesQuotation quotation = quotation(command.quotationId()).expire(command.today());
        repository.updateQuotation(quotation);
        audit.record(command.actor(), "SALES_QUOTATION_EXPIRED", quotation.id());
        return quotation;
    }

    @Transactional
    public SalesQuotation reviseQuotation(final SalesCommands.ReviseQuotation command) {
        authorization.requirePermission(command.actor(), "sales.quotation.revise");
        command.revisedLines().forEach(this::validateLine);
        final SalesQuotation revised = quotation(command.quotationId()).revise(command.revisedLines());
        repository.insertQuotationRevision(new SalesQuotationRevision(UUID.randomUUID(), revised.id(),
                revised.revision(), command.reason(), now(), command.actor()));
        repository.updateQuotation(revised);
        audit.record(command.actor(), "SALES_QUOTATION_REVISED", revised.id());
        return revised;
    }

    @Transactional
    public SalesOrder createSalesOrder(final SalesCommands.CreateSalesOrder command) {
        authorization.requirePermission(command.actor(), "sales.order.create");
        assertIdempotent(command.idempotencyKey());
        if (!command.directSales()) {
            final SalesQuotation quotation = quotation(command.quotationId());
            if (quotation.status() == SalesQuotationStatus.EXPIRED && !command.expiredQuotationOverride()) {
                throw new SalesConflictException("Expired quotation requires explicit conversion authorization.");
            }
            if (quotation.status() != SalesQuotationStatus.APPROVED
                    && quotation.status() != SalesQuotationStatus.EXPIRED) {
                throw new SalesConflictException("Sales order requires approved quotation.");
            }
            assertOrderDoesNotExceedQuotation(quotation, command.lines());
        } else {
            authorization.requirePermission(command.actor(), "sales.direct-order.create");
        }
        customer(command.customerId());
        authorization.requireCustomerScope(command.actor(), command.customerId());
        validateScope(command.companyId(), command.branchId(), command.warehouseId(), command.salesChannelId());
        masterData.requireCurrency(command.currencyId());
        command.lines().forEach(this::validateOrderLine);
        command.lines().forEach(line -> inventory.checkAvailability(line.skuId(), command.warehouseId(),
                line.orderedQuantity()));
        final SalesOrder order = repository.insertSalesOrder(new SalesOrder(UUID.randomUUID(), numbers.nextNumber("SO"),
                command.idempotencyKey(), command.quotationId(), command.customerId(), command.companyId(),
                command.branchId(), command.warehouseId(), command.salesChannelId(), command.currencyId(),
                SalesOrderStatus.DRAFT, 0, command.lines(), 0, command.requestedDeliveryDate(), now(),
                command.actor()));
        command.attachmentIds().forEach(attachmentId -> attachments.attach(order.id(), attachmentId));
        audit.record(command.actor(), "SALES_ORDER_CREATED", order.id());
        return order;
    }

    @Transactional
    public SalesOrder approveSalesOrder(final SalesCommands.ApproveSalesOrder command) {
        authorization.requirePermission(command.actor(), "sales.order.approve");
        final SalesOrder order = salesOrder(command.salesOrderId()).approve();
        repository.updateSalesOrder(order);
        audit.record(command.actor(), "SALES_ORDER_APPROVED", order.id());
        events.publish("SalesOrderApproved", order.id());
        return order;
    }

    @Transactional
    public SalesOrder amendSalesOrder(final SalesCommands.AmendSalesOrder command) {
        authorization.requirePermission(command.actor(), "sales.order.amend");
        command.revisedLines().forEach(this::validateOrderLine);
        final SalesOrder amended = salesOrder(command.salesOrderId()).amend(command.revisedLines());
        repository.insertOrderRevision(new SalesOrderRevision(UUID.randomUUID(), amended.id(), amended.revision(),
                command.reason(), now(), command.actor()));
        repository.updateSalesOrder(amended);
        audit.record(command.actor(), "SALES_ORDER_AMENDED", amended.id());
        return amended;
    }

    @Transactional
    public SalesOrder reserveInventory(final SalesCommands.ReserveInventory command) {
        authorization.requirePermission(command.actor(), "sales.order.reserve");
        final SalesOrder order = salesOrder(command.salesOrderId());
        assertLineExists(order, command.lineId());
        final SalesOrder updated = order.reserve(command.lineId(), command.quantity());
        inventory.requestReservation(updated.id(), command.lineId(), command.quantity());
        repository.updateSalesOrder(updated);
        audit.record(command.actor(), "SALES_ORDER_RESERVED", updated.id());
        return updated;
    }

    @Transactional
    public SalesOrder trackDelivery(final SalesCommands.TrackDelivery command) {
        authorization.requirePermission(command.actor(), "sales.order.delivery.track");
        final SalesOrder order = salesOrder(command.salesOrderId());
        assertLineExists(order, command.lineId());
        final SalesOrder updated = order.deliver(command.lineId(), command.quantity());
        inventory.requestDelivery(updated.id(), command.lineId(), command.quantity());
        repository.updateSalesOrder(updated);
        audit.record(command.actor(), "SALES_ORDER_DELIVERY_TRACKED", updated.id());
        return updated;
    }

    @Transactional
    public SalesOrder cancelSalesOrder(final SalesCommands.CancelSalesOrder command) {
        authorization.requirePermission(command.actor(), "sales.order.cancel");
        final SalesOrder cancelled = salesOrder(command.salesOrderId()).cancel();
        repository.updateSalesOrder(cancelled);
        audit.record(command.actor(), "SALES_ORDER_CANCELLED", cancelled.id());
        return cancelled;
    }

    @Transactional(readOnly = true)
    public List<Customer> customers() {
        return repository.listCustomers();
    }

    private void validateScope(final UUID companyId, final UUID branchId, final UUID warehouseId,
                               final UUID salesChannelId) {
        enterprise.requireCompanyBranchWarehouse(companyId, branchId, warehouseId);
        enterprise.requireSalesChannel(salesChannelId);
    }

    private void validateCommercialTerms(final UUID currencyId, final UUID paymentTermsId,
                                         final UUID shippingMethodId, final UUID incotermsId) {
        masterData.requireCurrency(currencyId);
        masterData.requirePaymentTerms(paymentTermsId);
        masterData.requireShippingMethod(shippingMethodId);
        masterData.requireIncoterms(incotermsId);
    }

    private void validateLine(final SalesLine line) {
        catalog.requireSku(line.productId(), line.skuId(), line.skuCode());
        masterData.requireUom(line.quantity().uomCode());
        if (line.taxCategoryId() != null) {
            masterData.requireTaxCategory(line.taxCategoryId());
        }
    }

    private void validateOrderLine(final SalesOrder.SalesOrderLine line) {
        catalog.requireSku(line.productId(), line.skuId(), line.skuCode());
        masterData.requireUom(line.orderedQuantity().uomCode());
        if (line.taxCategoryId() != null) {
            masterData.requireTaxCategory(line.taxCategoryId());
        }
    }

    private void assertOrderDoesNotExceedQuotation(final SalesQuotation quotation,
                                                   final List<SalesOrder.SalesOrderLine> orderLines) {
        for (final SalesOrder.SalesOrderLine orderLine : orderLines) {
            final SalesLine quotationLine = quotation.lines().stream()
                    .filter(line -> line.skuCode().equals(orderLine.skuCode())).findFirst()
                    .orElseThrow(() -> new SalesConflictException("Sales order SKU is not on quotation."));
            if (orderLine.orderedQuantity().isGreaterThan(quotationLine.quantity())) {
                throw new SalesConflictException("Sales order quantity exceeds approved quotation.");
            }
        }
    }

    private void assertLineExists(final SalesOrder order, final UUID lineId) {
        if (order.lines().stream().noneMatch(line -> line.id().equals(lineId))) {
            throw new SalesConflictException("Sales order line not found: " + lineId);
        }
    }

    private void assertIdempotent(final String key) {
        if (repository.idempotencyKeyExists(key)) {
            throw new SalesConflictException("Duplicate idempotency key: " + key);
        }
    }

    private Customer customer(final UUID id) {
        return repository.findCustomer(id).orElseThrow(() -> new SalesNotFoundException("Customer not found: " + id));
    }

    private SalesQuotation quotation(final UUID id) {
        return repository.findQuotation(id).orElseThrow(() ->
                new SalesNotFoundException("Sales quotation not found: " + id));
    }

    private SalesOrder salesOrder(final UUID id) {
        return repository.findSalesOrder(id).orElseThrow(() ->
                new SalesNotFoundException("Sales order not found: " + id));
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
