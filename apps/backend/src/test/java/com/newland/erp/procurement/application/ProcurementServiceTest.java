package com.newland.erp.procurement.application;

import com.newland.erp.procurement.domain.ProcurementConflictException;
import com.newland.erp.procurement.domain.ProcurementLine;
import com.newland.erp.procurement.domain.ProcurementQuantity;
import com.newland.erp.procurement.domain.PurchaseOrder;
import com.newland.erp.procurement.domain.PurchaseOrderStatus;
import com.newland.erp.procurement.domain.RequisitionStatus;
import com.newland.erp.procurement.domain.Supplier;
import com.newland.erp.procurement.domain.SupplierQuotationStatus;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class ProcurementServiceTest {
    private final InMemoryProcurementRepository repository = new InMemoryProcurementRepository();
    private final List<UUID> receiptRequests = new ArrayList<>();
    private final ProcurementService service = service();

    @Test
    void supplierCreationEnforcesUniqueCodeAndInvalidReferenceRejection() {
        service.createSupplier(supplier("idem-supplier-1", "SUP-1"));

        assertThatThrownBy(() -> service.createSupplier(supplier("idem-supplier-2", "SUP-1")))
                .isInstanceOf(ProcurementConflictException.class);
        assertThatThrownBy(() -> service.submitRequisition(new ProcurementCommands.SubmitRequisition(
                "bad-product", ids(), ids(), ids(), List.of(line(null, "SKU-1")), List.of(), "architect")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requisitionApprovalRejectionResubmissionAndRfqInvitationFlow() {
        final Supplier firstSupplier = service.createSupplier(supplier("idem-supplier-1", "SUP-1"));
        final Supplier secondSupplier = service.createSupplier(supplier("idem-supplier-2", "SUP-2"));
        final var rejected = service.rejectRequisition(new ProcurementCommands.RejectRequisition(
                service.submitRequisition(requisition("req-1")).id(), "architect"));

        final var resubmitted = service.resubmitRequisition(new ProcurementCommands.ResubmitRequisition(
                rejected.id(), "req-2", "architect"));
        final var approved = service.approveRequisition(new ProcurementCommands.ApproveRequisition(
                resubmitted.id(), "architect"));
        final var rfq = service.createRfq(new ProcurementCommands.CreateRfq("rfq-1", approved.id(),
                List.of(firstSupplier.id(), secondSupplier.id()), "architect"));

        assertThat(approved.status()).isEqualTo(RequisitionStatus.APPROVED);
        assertThat(rfq.invitedSupplierIds()).containsExactly(firstSupplier.id(), secondSupplier.id());
        assertThatThrownBy(() -> service.approveRequisition(new ProcurementCommands.ApproveRequisition(
                approved.id(), "architect"))).isInstanceOf(ProcurementConflictException.class);
    }

    @Test
    void quotationSubmissionAndAuditableComparisonPreserveTerms() {
        final Supplier supplier = service.createSupplier(supplier("idem-supplier-1", "SUP-1"));
        final var requisition = service.approveRequisition(new ProcurementCommands.ApproveRequisition(
                service.submitRequisition(requisition("req-1")).id(), "architect"));
        final var rfq = service.createRfq(new ProcurementCommands.CreateRfq("rfq-1", requisition.id(),
                List.of(supplier.id()), "architect"));

        final var quotation = service.submitQuotation(new ProcurementCommands.SubmitQuotation("quote-1", rfq.id(),
                supplier.id(), ids(), ids(), ids(), ids(), List.of(line(ids(), "SKU-1")), List.of(), "architect"));
        final var comparison = service.compareQuotations(new ProcurementCommands.CompareQuotations(rfq.id(),
                quotation.id(), List.of(quotation.id()), "architect"));

        assertThat(quotation.status()).isEqualTo(SupplierQuotationStatus.SUBMITTED);
        assertThat(comparison.selectedQuotationId()).isEqualTo(quotation.id());
    }

    @Test
    void purchaseOrderApprovalPartialDeliveryAmendmentCancellationAndInventoryPort() {
        final Supplier supplier = service.createSupplier(supplier("idem-supplier-1", "SUP-1"));
        final var requisition = service.approveRequisition(new ProcurementCommands.ApproveRequisition(
                service.submitRequisition(requisition("req-1")).id(), "architect"));
        final PurchaseOrder order = service.createPurchaseOrder(order("po-1", requisition.id(), supplier.id()));

        final PurchaseOrder approved = service.approvePurchaseOrder(new ProcurementCommands.ApprovePurchaseOrder(
                order.id(), "architect"));
        final PurchaseOrder amended = service.amendPurchaseOrder(new ProcurementCommands.AmendPurchaseOrder(
                approved.id(), "supplier changed pack size", List.of(poLine("12")), "architect"));
        final PurchaseOrder reapproved = service.approvePurchaseOrder(new ProcurementCommands.ApprovePurchaseOrder(
                amended.id(), "architect"));
        final UUID lineId = reapproved.lines().getFirst().id();
        final PurchaseOrder partial = service.recordPartialDelivery(new ProcurementCommands.RecordPartialDelivery(
                reapproved.id(), lineId, qty("4"), "architect"));
        final PurchaseOrder cancelled = service.cancelPurchaseOrder(new ProcurementCommands.CancelPurchaseOrder(
                partial.id(), "architect"));

        assertThat(partial.status()).isEqualTo(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        assertThat(receiptRequests).contains(lineId);
        assertThat(repository.listRevisions(order.id())).hasSize(1);
        assertThat(cancelled.lines().getFirst().receivedQuantity().value()).isEqualByComparingTo("4");
        assertThat(cancelled.status()).isEqualTo(PurchaseOrderStatus.CANCELLED);
    }

    @Test
    void unauthorizedApprovalDuplicateIdempotencyAndRollbackAfterFailureAreEnforced() {
        final ProcurementService locked = service(false);
        final Supplier supplier = service.createSupplier(supplier("idem-supplier-1", "SUP-1"));
        final var requisition = service.approveRequisition(new ProcurementCommands.ApproveRequisition(
                service.submitRequisition(requisition("req-1")).id(), "architect"));
        service.createPurchaseOrder(order("po-1", requisition.id(), supplier.id()));

        assertThatThrownBy(() -> service.createPurchaseOrder(order("po-1", requisition.id(), supplier.id())))
                .isInstanceOf(ProcurementConflictException.class);
        assertThatThrownBy(() -> locked.approvePurchaseOrder(new ProcurementCommands.ApprovePurchaseOrder(
                UUID.randomUUID(), "architect"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.createRfq(new ProcurementCommands.CreateRfq("rfq-fail",
                UUID.randomUUID(), List.of(supplier.id()), "architect"))).isInstanceOf(RuntimeException.class);
        assertThat(repository.rfqs).isEmpty();
    }

    private ProcurementService service() {
        return service(true);
    }

    private ProcurementService service(final boolean authorized) {
        final AtomicLong sequence = new AtomicLong();
        return new ProcurementService(repository, (productId, skuId, skuCode) -> {
            if (productId == null || skuId == null || skuCode == null) {
                throw new IllegalArgumentException("invalid product");
            }
        }, new AllowingMasterDataPort(), (companyId, branchId, warehouseId) -> {
            if (companyId == null || branchId == null || warehouseId == null) {
                throw new IllegalArgumentException("invalid warehouse scope");
            }
        }, (purchaseOrderId, lineId, quantity) -> receiptRequests.add(lineId),
                prefix -> prefix + "-" + sequence.incrementAndGet(), (actor, action, targetId) -> {
                }, (eventType, aggregateId) -> {
                }, (aggregateId, attachmentId) -> {
                }, new TestAuthorizationPort(authorized),
                Clock.fixed(Instant.parse("2026-07-21T00:00:00Z"), ZoneOffset.UTC));
    }

    private static ProcurementCommands.CreateSupplier supplier(final String key, final String code) {
        return new ProcurementCommands.CreateSupplier(key, code, "Supplier " + code, List.of(), List.of(),
                List.of(new Supplier.SupplierProductReference(UUID.randomUUID(), ids(), ids(), "SUP-SKU", 7,
                        qty("5"), "carton")), List.of(UUID.randomUUID()), "architect");
    }

    private static ProcurementCommands.SubmitRequisition requisition(final String key) {
        return new ProcurementCommands.SubmitRequisition(key, ids(), ids(), ids(), List.of(line(ids(), "SKU-1")),
                List.of(UUID.randomUUID()), "architect");
    }

    private static ProcurementCommands.CreatePurchaseOrder order(final String key, final UUID requisitionId,
                                                                 final UUID supplierId) {
        return new ProcurementCommands.CreatePurchaseOrder(key, requisitionId, false, supplierId, ids(), ids(), ids(),
                ids(), List.of(poLine("10")), LocalDate.parse("2026-08-01"), List.of(UUID.randomUUID()),
                "architect");
    }

    private static ProcurementLine line(final UUID productId, final String skuCode) {
        return new ProcurementLine(UUID.randomUUID(), productId, ids(), skuCode, qty("10"), BigDecimal.TEN, ids());
    }

    private static PurchaseOrder.PurchaseOrderLine poLine(final String quantity) {
        final ProcurementQuantity zero = qty("0");
        return new PurchaseOrder.PurchaseOrderLine(UUID.randomUUID(), ids(), ids(), "SKU-1", qty(quantity), zero,
                zero, ids());
    }

    private static ProcurementQuantity qty(final String value) {
        return new ProcurementQuantity(new BigDecimal(value), "EA");
    }

    private static UUID ids() {
        return UUID.randomUUID();
    }

    private static final class AllowingMasterDataPort implements ProcurementPorts.MasterDataPort {
        @Override
        public void requireUom(final String uomCode) {
            if (uomCode == null) {
                throw new IllegalArgumentException("invalid UOM");
            }
        }

        @Override
        public void requireCurrency(final UUID currencyId) {
        }

        @Override
        public void requireTaxCategory(final UUID taxCategoryId) {
        }

        @Override
        public void requirePaymentTerms(final UUID paymentTermsId) {
        }

        @Override
        public void requireShippingMethod(final UUID shippingMethodId) {
        }

        @Override
        public void requireIncoterms(final UUID incotermsId) {
        }
    }

    private record TestAuthorizationPort(boolean authorized) implements ProcurementPorts.AuthorizationPort {
        @Override
        public void requirePermission(final String actor, final String capability) {
            if (!authorized) {
                throw new IllegalArgumentException("unauthorized");
            }
        }

        @Override
        public void requireSupplierScope(final String actor, final UUID supplierId) {
            requirePermission(actor, "supplier");
        }
    }
}
