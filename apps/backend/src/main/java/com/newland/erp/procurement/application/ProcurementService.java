package com.newland.erp.procurement.application;

import com.newland.erp.procurement.domain.ProcurementConflictException;
import com.newland.erp.procurement.domain.ProcurementLine;
import com.newland.erp.procurement.domain.ProcurementNotFoundException;
import com.newland.erp.procurement.domain.PurchaseOrder;
import com.newland.erp.procurement.domain.PurchaseOrderRevision;
import com.newland.erp.procurement.domain.PurchaseOrderStatus;
import com.newland.erp.procurement.domain.PurchaseRequisition;
import com.newland.erp.procurement.domain.QuotationComparison;
import com.newland.erp.procurement.domain.RequisitionStatus;
import com.newland.erp.procurement.domain.RequestForQuotation;
import com.newland.erp.procurement.domain.RfqStatus;
import com.newland.erp.procurement.domain.Supplier;
import com.newland.erp.procurement.domain.SupplierQuotation;
import com.newland.erp.procurement.domain.SupplierQuotationStatus;
import com.newland.erp.procurement.domain.SupplierStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public final class ProcurementService {
    private final ProcurementRepository repository;
    private final ProcurementPorts.ProductCatalogPort catalog;
    private final ProcurementPorts.MasterDataPort masterData;
    private final ProcurementPorts.EnterpriseScopePort enterprise;
    private final ProcurementPorts.InventoryReceiptPort inventory;
    private final ProcurementPorts.NumberSeriesPort numbers;
    private final ProcurementPorts.AuditPort audit;
    private final ProcurementPorts.DomainEventPort events;
    private final ProcurementPorts.AttachmentPort attachments;
    private final ProcurementPorts.AuthorizationPort authorization;
    private final Clock clock;

    public ProcurementService(final ProcurementRepository procurementRepository,
                              final ProcurementPorts.ProductCatalogPort productCatalogPort,
                              final ProcurementPorts.MasterDataPort masterDataPort,
                              final ProcurementPorts.EnterpriseScopePort enterpriseScopePort,
                              final ProcurementPorts.InventoryReceiptPort inventoryReceiptPort,
                              final ProcurementPorts.NumberSeriesPort numberSeriesPort,
                              final ProcurementPorts.AuditPort auditPort,
                              final ProcurementPorts.DomainEventPort domainEventPort,
                              final ProcurementPorts.AttachmentPort attachmentPort,
                              final ProcurementPorts.AuthorizationPort authorizationPort,
                              final Clock systemClock) {
        this.repository = procurementRepository;
        this.catalog = productCatalogPort;
        this.masterData = masterDataPort;
        this.enterprise = enterpriseScopePort;
        this.inventory = inventoryReceiptPort;
        this.numbers = numberSeriesPort;
        this.audit = auditPort;
        this.events = domainEventPort;
        this.attachments = attachmentPort;
        this.authorization = authorizationPort;
        this.clock = systemClock;
    }

    @Transactional
    public Supplier createSupplier(final ProcurementCommands.CreateSupplier command) {
        authorization.requirePermission(command.actor(), "procurement.supplier.create");
        assertIdempotent(command.idempotencyKey());
        if (repository.supplierCodeExists(command.supplierCode().trim().toUpperCase())) {
            throw new ProcurementConflictException("Duplicate supplier code: " + command.supplierCode());
        }
        command.productReferences().forEach(reference -> {
            catalog.requireSku(reference.productId(), reference.skuId(), reference.supplierSku());
            masterData.requireUom(reference.minimumOrderQuantity().uomCode());
        });
        final Supplier supplier = repository.insertSupplier(new Supplier(UUID.randomUUID(), command.supplierCode(),
                command.name(), SupplierStatus.ACTIVE, command.contacts(), command.addresses(),
                command.productReferences(), now()));
        command.attachmentIds().forEach(attachmentId -> attachments.attach(supplier.id(), attachmentId));
        audit.record(command.actor(), "PROCUREMENT_SUPPLIER_CREATED", supplier.id());
        events.publish("ProcurementSupplierCreated", supplier.id());
        return supplier;
    }

    @Transactional
    public PurchaseRequisition submitRequisition(final ProcurementCommands.SubmitRequisition command) {
        authorization.requirePermission(command.actor(), "procurement.requisition.submit");
        assertIdempotent(command.idempotencyKey());
        enterprise.requireCompanyBranchWarehouse(command.companyId(), command.branchId(), command.warehouseId());
        command.lines().forEach(this::validateLine);
        final PurchaseRequisition requisition = repository.insertRequisition(new PurchaseRequisition(
                UUID.randomUUID(), numbers.nextNumber("PR"), command.idempotencyKey(), command.companyId(),
                command.branchId(), command.warehouseId(), RequisitionStatus.SUBMITTED, 0, command.lines(), now(),
                command.actor()));
        command.attachmentIds().forEach(attachmentId -> attachments.attach(requisition.id(), attachmentId));
        audit.record(command.actor(), "PROCUREMENT_REQUISITION_SUBMITTED", requisition.id());
        events.publish("ProcurementRequisitionSubmitted", requisition.id());
        return requisition;
    }

    @Transactional
    public PurchaseRequisition approveRequisition(final ProcurementCommands.ApproveRequisition command) {
        authorization.requirePermission(command.actor(), "procurement.requisition.approve");
        final PurchaseRequisition requisition = requisition(command.requisitionId()).approve();
        repository.updateRequisition(requisition);
        audit.record(command.actor(), "PROCUREMENT_REQUISITION_APPROVED", requisition.id());
        return requisition;
    }

    @Transactional
    public PurchaseRequisition rejectRequisition(final ProcurementCommands.RejectRequisition command) {
        authorization.requirePermission(command.actor(), "procurement.requisition.reject");
        final PurchaseRequisition requisition = requisition(command.requisitionId()).reject();
        repository.updateRequisition(requisition);
        audit.record(command.actor(), "PROCUREMENT_REQUISITION_REJECTED", requisition.id());
        return requisition;
    }

    @Transactional
    public PurchaseRequisition resubmitRequisition(final ProcurementCommands.ResubmitRequisition command) {
        authorization.requirePermission(command.actor(), "procurement.requisition.submit");
        assertIdempotent(command.idempotencyKey());
        final PurchaseRequisition requisition = requisition(command.requisitionId()).resubmit(command.idempotencyKey());
        repository.updateRequisition(requisition);
        audit.record(command.actor(), "PROCUREMENT_REQUISITION_RESUBMITTED", requisition.id());
        return requisition;
    }

    @Transactional
    public RequestForQuotation createRfq(final ProcurementCommands.CreateRfq command) {
        authorization.requirePermission(command.actor(), "procurement.rfq.create");
        assertIdempotent(command.idempotencyKey());
        final PurchaseRequisition requisition = requisition(command.requisitionId());
        if (requisition.status() != RequisitionStatus.APPROVED) {
            throw new ProcurementConflictException("RFQ requires an approved requisition.");
        }
        command.invitedSupplierIds().forEach(this::supplier);
        final RequestForQuotation rfq = repository.insertRfq(new RequestForQuotation(UUID.randomUUID(),
                numbers.nextNumber("RFQ"), command.idempotencyKey(), requisition.id(), RfqStatus.SENT,
                command.invitedSupplierIds(), now()));
        audit.record(command.actor(), "PROCUREMENT_RFQ_SENT", rfq.id());
        events.publish("ProcurementRfqSent", rfq.id());
        return rfq;
    }

    @Transactional
    public SupplierQuotation submitQuotation(final ProcurementCommands.SubmitQuotation command) {
        authorization.requirePermission(command.actor(), "procurement.quotation.submit");
        assertIdempotent(command.idempotencyKey());
        rfq(command.rfqId());
        supplier(command.supplierId());
        authorization.requireSupplierScope(command.actor(), command.supplierId());
        masterData.requireCurrency(command.currencyId());
        masterData.requirePaymentTerms(command.paymentTermsId());
        masterData.requireShippingMethod(command.shippingMethodId());
        masterData.requireIncoterms(command.incotermsId());
        command.lines().forEach(this::validateLine);
        final SupplierQuotation quotation = repository.insertQuotation(new SupplierQuotation(UUID.randomUUID(),
                numbers.nextNumber("SQ"), command.idempotencyKey(), command.rfqId(), command.supplierId(),
                command.currencyId(), command.paymentTermsId(), command.shippingMethodId(), command.incotermsId(),
                SupplierQuotationStatus.SUBMITTED, command.lines(), now()));
        command.attachmentIds().forEach(attachmentId -> attachments.attach(quotation.id(), attachmentId));
        audit.record(command.actor(), "PROCUREMENT_SUPPLIER_QUOTATION_SUBMITTED", quotation.id());
        return quotation;
    }

    @Transactional
    public QuotationComparison compareQuotations(final ProcurementCommands.CompareQuotations command) {
        authorization.requirePermission(command.actor(), "procurement.quotation.compare");
        command.quotationIds().forEach(this::quotation);
        quotation(command.selectedQuotationId());
        final QuotationComparison comparison = repository.insertComparison(new QuotationComparison(
                UUID.randomUUID(), command.rfqId(), command.selectedQuotationId(), command.quotationIds(), now(),
                command.actor()));
        audit.record(command.actor(), "PROCUREMENT_QUOTATIONS_COMPARED", comparison.id());
        return comparison;
    }

    @Transactional
    public PurchaseOrder createPurchaseOrder(final ProcurementCommands.CreatePurchaseOrder command) {
        authorization.requirePermission(command.actor(), "procurement.purchase-order.create");
        assertIdempotent(command.idempotencyKey());
        if (!command.directPurchase()) {
            final PurchaseRequisition requisition = requisition(command.requisitionId());
            if (requisition.status() != RequisitionStatus.APPROVED) {
                throw new ProcurementConflictException("Purchase order requires approved requisition.");
            }
        } else {
            authorization.requirePermission(command.actor(), "procurement.direct-purchase.create");
        }
        supplier(command.supplierId());
        authorization.requireSupplierScope(command.actor(), command.supplierId());
        enterprise.requireCompanyBranchWarehouse(command.companyId(), command.branchId(), command.warehouseId());
        masterData.requireCurrency(command.currencyId());
        command.lines().forEach(this::validatePurchaseOrderLine);
        final PurchaseOrder order = repository.insertPurchaseOrder(new PurchaseOrder(UUID.randomUUID(),
                numbers.nextNumber("PO"), command.idempotencyKey(), command.requisitionId(), command.supplierId(),
                command.companyId(), command.branchId(), command.warehouseId(), command.currencyId(),
                PurchaseOrderStatus.DRAFT, 0, command.lines(), command.expectedDeliveryDate(), now(),
                command.actor()));
        command.attachmentIds().forEach(attachmentId -> attachments.attach(order.id(), attachmentId));
        audit.record(command.actor(), "PROCUREMENT_PURCHASE_ORDER_CREATED", order.id());
        return order;
    }

    @Transactional
    public PurchaseOrder approvePurchaseOrder(final ProcurementCommands.ApprovePurchaseOrder command) {
        authorization.requirePermission(command.actor(), "procurement.purchase-order.approve");
        final PurchaseOrder approved = purchaseOrder(command.purchaseOrderId()).approve();
        repository.updatePurchaseOrder(approved);
        audit.record(command.actor(), "PROCUREMENT_PURCHASE_ORDER_APPROVED", approved.id());
        events.publish("ProcurementPurchaseOrderApproved", approved.id());
        return approved;
    }

    @Transactional
    public PurchaseOrder recordPartialDelivery(final ProcurementCommands.RecordPartialDelivery command) {
        authorization.requirePermission(command.actor(), "procurement.purchase-order.receive");
        final PurchaseOrder order = purchaseOrder(command.purchaseOrderId());
        if (order.status() == PurchaseOrderStatus.CANCELLED) {
            throw new ProcurementConflictException("Cancelled purchase orders cannot receive deliveries.");
        }
        final PurchaseOrder updated = order.receive(command.lineId(), command.quantity());
        repository.updatePurchaseOrder(updated);
        inventory.requestReceipt(updated.id(), command.lineId(), command.quantity());
        audit.record(command.actor(), "PROCUREMENT_PARTIAL_DELIVERY_TRACKED", updated.id());
        return updated;
    }

    @Transactional
    public PurchaseOrder amendPurchaseOrder(final ProcurementCommands.AmendPurchaseOrder command) {
        authorization.requirePermission(command.actor(), "procurement.purchase-order.amend");
        final PurchaseOrder order = purchaseOrder(command.purchaseOrderId());
        command.revisedLines().forEach(this::validatePurchaseOrderLine);
        final PurchaseOrder amended = order.amend(command.revisedLines());
        repository.insertRevision(new PurchaseOrderRevision(UUID.randomUUID(), order.id(), amended.revision(),
                command.reason(), now(), command.actor()));
        repository.updatePurchaseOrder(amended);
        audit.record(command.actor(), "PROCUREMENT_PURCHASE_ORDER_AMENDED", amended.id());
        return amended;
    }

    @Transactional
    public PurchaseOrder cancelPurchaseOrder(final ProcurementCommands.CancelPurchaseOrder command) {
        authorization.requirePermission(command.actor(), "procurement.purchase-order.cancel");
        final PurchaseOrder cancelled = purchaseOrder(command.purchaseOrderId()).cancel();
        repository.updatePurchaseOrder(cancelled);
        audit.record(command.actor(), "PROCUREMENT_PURCHASE_ORDER_CANCELLED", cancelled.id());
        return cancelled;
    }

    @Transactional(readOnly = true)
    public List<Supplier> suppliers() {
        return repository.listSuppliers();
    }

    private void validateLine(final ProcurementLine line) {
        catalog.requireSku(line.productId(), line.skuId(), line.skuCode());
        masterData.requireUom(line.quantity().uomCode());
        if (line.taxCategoryId() != null) {
            masterData.requireTaxCategory(line.taxCategoryId());
        }
    }

    private void validatePurchaseOrderLine(final PurchaseOrder.PurchaseOrderLine line) {
        catalog.requireSku(line.productId(), line.skuId(), line.skuCode());
        masterData.requireUom(line.orderedQuantity().uomCode());
        if (line.taxCategoryId() != null) {
            masterData.requireTaxCategory(line.taxCategoryId());
        }
    }

    private void assertIdempotent(final String key) {
        if (repository.idempotencyKeyExists(key)) {
            throw new ProcurementConflictException("Duplicate idempotency key: " + key);
        }
    }

    private PurchaseRequisition requisition(final UUID id) {
        return repository.findRequisition(id).orElseThrow(() ->
                new ProcurementNotFoundException("Purchase requisition not found: " + id));
    }

    private RequestForQuotation rfq(final UUID id) {
        return repository.findRfq(id).orElseThrow(() -> new ProcurementNotFoundException("RFQ not found: " + id));
    }

    private SupplierQuotation quotation(final UUID id) {
        return repository.findQuotation(id).orElseThrow(() ->
                new ProcurementNotFoundException("Supplier quotation not found: " + id));
    }

    private PurchaseOrder purchaseOrder(final UUID id) {
        return repository.findPurchaseOrder(id).orElseThrow(() ->
                new ProcurementNotFoundException("Purchase order not found: " + id));
    }

    private Supplier supplier(final UUID id) {
        return repository.findSupplier(id).orElseThrow(() ->
                new ProcurementNotFoundException("Supplier not found: " + id));
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
