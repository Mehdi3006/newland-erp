package com.newland.erp.procurement.application;

import com.newland.erp.procurement.domain.PurchaseOrder;
import com.newland.erp.procurement.domain.PurchaseOrderRevision;
import com.newland.erp.procurement.domain.PurchaseRequisition;
import com.newland.erp.procurement.domain.QuotationComparison;
import com.newland.erp.procurement.domain.RequestForQuotation;
import com.newland.erp.procurement.domain.Supplier;
import com.newland.erp.procurement.domain.SupplierQuotation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcurementRepository {
    boolean idempotencyKeyExists(String idempotencyKey);

    boolean supplierCodeExists(String supplierCode);

    Supplier insertSupplier(Supplier supplier);

    Optional<Supplier> findSupplier(UUID supplierId);

    List<Supplier> listSuppliers();

    PurchaseRequisition insertRequisition(PurchaseRequisition requisition);

    PurchaseRequisition updateRequisition(PurchaseRequisition requisition);

    Optional<PurchaseRequisition> findRequisition(UUID requisitionId);

    RequestForQuotation insertRfq(RequestForQuotation rfq);

    Optional<RequestForQuotation> findRfq(UUID rfqId);

    SupplierQuotation insertQuotation(SupplierQuotation quotation);

    Optional<SupplierQuotation> findQuotation(UUID quotationId);

    QuotationComparison insertComparison(QuotationComparison comparison);

    PurchaseOrder insertPurchaseOrder(PurchaseOrder purchaseOrder);

    PurchaseOrder updatePurchaseOrder(PurchaseOrder purchaseOrder);

    Optional<PurchaseOrder> findPurchaseOrder(UUID purchaseOrderId);

    PurchaseOrderRevision insertRevision(PurchaseOrderRevision revision);

    List<PurchaseOrderRevision> listRevisions(UUID purchaseOrderId);
}
