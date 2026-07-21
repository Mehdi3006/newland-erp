package com.newland.erp.procurement.application;

import com.newland.erp.procurement.domain.PurchaseOrder;
import com.newland.erp.procurement.domain.PurchaseOrderRevision;
import com.newland.erp.procurement.domain.PurchaseRequisition;
import com.newland.erp.procurement.domain.QuotationComparison;
import com.newland.erp.procurement.domain.RequestForQuotation;
import com.newland.erp.procurement.domain.Supplier;
import com.newland.erp.procurement.domain.SupplierQuotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class InMemoryProcurementRepository implements ProcurementRepository {
    final List<Supplier> suppliers = new ArrayList<>();
    final List<PurchaseRequisition> requisitions = new ArrayList<>();
    final List<RequestForQuotation> rfqs = new ArrayList<>();
    final List<SupplierQuotation> quotations = new ArrayList<>();
    final List<QuotationComparison> comparisons = new ArrayList<>();
    final List<PurchaseOrder> purchaseOrders = new ArrayList<>();
    final List<PurchaseOrderRevision> revisions = new ArrayList<>();

    @Override
    public boolean idempotencyKeyExists(final String idempotencyKey) {
        return suppliers.stream().anyMatch(item -> item.idempotencyKey().equals(idempotencyKey))
                || requisitions.stream().anyMatch(item -> item.idempotencyKey().equals(idempotencyKey))
                || rfqs.stream().anyMatch(item -> item.idempotencyKey().equals(idempotencyKey))
                || quotations.stream().anyMatch(item -> item.idempotencyKey().equals(idempotencyKey))
                || purchaseOrders.stream().anyMatch(item -> item.idempotencyKey().equals(idempotencyKey));
    }

    @Override
    public boolean supplierCodeExists(final String supplierCode) {
        return suppliers.stream().anyMatch(item -> item.supplierCode().equals(supplierCode));
    }

    @Override
    public Supplier insertSupplier(final Supplier supplier) {
        suppliers.add(supplier);
        return supplier;
    }

    @Override
    public Optional<Supplier> findSupplier(final UUID supplierId) {
        return suppliers.stream().filter(item -> item.id().equals(supplierId)).findFirst();
    }

    @Override
    public List<Supplier> listSuppliers() {
        return List.copyOf(suppliers);
    }

    @Override
    public PurchaseRequisition insertRequisition(final PurchaseRequisition requisition) {
        requisitions.add(requisition);
        return requisition;
    }

    @Override
    public PurchaseRequisition updateRequisition(final PurchaseRequisition requisition) {
        requisitions.removeIf(item -> item.id().equals(requisition.id()));
        requisitions.add(requisition);
        return requisition;
    }

    @Override
    public Optional<PurchaseRequisition> findRequisition(final UUID requisitionId) {
        return requisitions.stream().filter(item -> item.id().equals(requisitionId)).findFirst();
    }

    @Override
    public RequestForQuotation insertRfq(final RequestForQuotation rfq) {
        rfqs.add(rfq);
        return rfq;
    }

    @Override
    public Optional<RequestForQuotation> findRfq(final UUID rfqId) {
        return rfqs.stream().filter(item -> item.id().equals(rfqId)).findFirst();
    }

    @Override
    public SupplierQuotation insertQuotation(final SupplierQuotation quotation) {
        quotations.add(quotation);
        return quotation;
    }

    @Override
    public Optional<SupplierQuotation> findQuotation(final UUID quotationId) {
        return quotations.stream().filter(item -> item.id().equals(quotationId)).findFirst();
    }

    @Override
    public QuotationComparison insertComparison(final QuotationComparison comparison) {
        comparisons.add(comparison);
        return comparison;
    }

    @Override
    public PurchaseOrder insertPurchaseOrder(final PurchaseOrder purchaseOrder) {
        purchaseOrders.add(purchaseOrder);
        return purchaseOrder;
    }

    @Override
    public PurchaseOrder updatePurchaseOrder(final PurchaseOrder purchaseOrder) {
        purchaseOrders.removeIf(item -> item.id().equals(purchaseOrder.id()));
        purchaseOrders.add(purchaseOrder);
        return purchaseOrder;
    }

    @Override
    public Optional<PurchaseOrder> findPurchaseOrder(final UUID purchaseOrderId) {
        return purchaseOrders.stream().filter(item -> item.id().equals(purchaseOrderId)).findFirst();
    }

    @Override
    public PurchaseOrderRevision insertRevision(final PurchaseOrderRevision revision) {
        revisions.add(revision);
        return revision;
    }

    @Override
    public List<PurchaseOrderRevision> listRevisions(final UUID purchaseOrderId) {
        return revisions.stream().filter(item -> item.purchaseOrderId().equals(purchaseOrderId)).toList();
    }
}
