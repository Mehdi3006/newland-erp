package com.newland.erp.procurement.infrastructure;

import com.newland.erp.procurement.application.ProcurementRepository;
import com.newland.erp.procurement.domain.ProcurementLine;
import com.newland.erp.procurement.domain.ProcurementQuantity;
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

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public final class JooqProcurementRepository implements ProcurementRepository {
    private final DSLContext dsl;

    public JooqProcurementRepository(final DSLContext dslContext) {
        this.dsl = dslContext;
    }

    @Override
    public boolean idempotencyKeyExists(final String idempotencyKey) {
        return dsl.fetchExists(table("procurement_purchase_requisition"), text("idempotency_key").eq(idempotencyKey))
                || dsl.fetchExists(table("procurement_rfq"), text("idempotency_key").eq(idempotencyKey))
                || dsl.fetchExists(table("procurement_supplier_quotation"),
                text("idempotency_key").eq(idempotencyKey))
                || dsl.fetchExists(table("procurement_purchase_order"), text("idempotency_key").eq(idempotencyKey));
    }

    @Override
    public boolean supplierCodeExists(final String supplierCode) {
        return dsl.fetchExists(table("procurement_supplier"), text("supplier_code").eq(supplierCode));
    }

    @Override
    public Supplier insertSupplier(final Supplier supplier) {
        dsl.insertInto(table("procurement_supplier"))
                .columns(id(), text("supplier_code"), text("name"), text("status"), instant("created_at"))
                .values(supplier.id(), supplier.supplierCode(), supplier.name(), supplier.status().name(),
                        supplier.createdAt())
                .execute();
        supplier.contacts().forEach(contact -> dsl.insertInto(table("procurement_supplier_contact"))
                .columns(id(), uuid("supplier_id"), text("name"), text("email"), text("phone"))
                .values(contact.id(), supplier.id(), contact.name(), contact.email(), contact.phone()).execute());
        supplier.addresses().forEach(address -> dsl.insertInto(table("procurement_supplier_address"))
                .columns(id(), uuid("supplier_id"), uuid("country_id"), uuid("province_id"), uuid("city_id"),
                        text("address_line"))
                .values(address.id(), supplier.id(), address.countryId(), address.provinceId(), address.cityId(),
                        address.addressLine()).execute());
        supplier.productReferences().forEach(reference -> dsl.insertInto(
                table("procurement_supplier_product_reference"))
                .columns(id(), uuid("supplier_id"), uuid("product_id"), uuid("sku_id"), text("supplier_sku"),
                        integer("lead_time_days"), decimal("minimum_order_quantity"), text("uom_code"),
                        text("packaging_information"))
                .values(reference.id(), supplier.id(), reference.productId(), reference.skuId(),
                        reference.supplierSku(), reference.leadTimeDays(),
                        reference.minimumOrderQuantity().value(), reference.minimumOrderQuantity().uomCode(),
                        reference.packagingInformation()).execute());
        return supplier;
    }

    @Override
    public Optional<Supplier> findSupplier(final UUID supplierId) {
        return dsl.selectFrom(table("procurement_supplier")).where(id().eq(supplierId)).fetchOptional(this::supplier);
    }

    @Override
    public List<Supplier> listSuppliers() {
        return dsl.selectFrom(table("procurement_supplier")).orderBy(text("supplier_code")).fetch(this::supplier);
    }

    @Override
    public PurchaseRequisition insertRequisition(final PurchaseRequisition requisition) {
        dsl.insertInto(table("procurement_purchase_requisition"))
                .columns(id(), text("requisition_number"), text("idempotency_key"), uuid("company_id"),
                        uuid("branch_id"), uuid("warehouse_id"), text("status"), integer("revision"),
                        instant("created_at"), text("actor"))
                .values(requisition.id(), requisition.requisitionNumber(), requisition.idempotencyKey(),
                        requisition.companyId(), requisition.branchId(), requisition.warehouseId(),
                        requisition.status().name(), requisition.revision(), requisition.createdAt(),
                        requisition.actor())
                .execute();
        requisition.lines().forEach(line -> insertProcurementLine("procurement_purchase_requisition_line",
                "requisition_id", requisition.id(), line));
        return requisition;
    }

    @Override
    public PurchaseRequisition updateRequisition(final PurchaseRequisition requisition) {
        dsl.update(table("procurement_purchase_requisition")).set(text("status"), requisition.status().name())
                .set(integer("revision"), requisition.revision()).set(text("idempotency_key"),
                        requisition.idempotencyKey()).where(id().eq(requisition.id())).execute();
        return requisition;
    }

    @Override
    public Optional<PurchaseRequisition> findRequisition(final UUID requisitionId) {
        return dsl.selectFrom(table("procurement_purchase_requisition")).where(id().eq(requisitionId))
                .fetchOptional(this::requisition);
    }

    @Override
    public RequestForQuotation insertRfq(final RequestForQuotation rfq) {
        dsl.insertInto(table("procurement_rfq"))
                .columns(id(), text("rfq_number"), text("idempotency_key"), uuid("requisition_id"),
                        text("status"), instant("created_at"))
                .values(rfq.id(), rfq.rfqNumber(), rfq.idempotencyKey(), rfq.requisitionId(), rfq.status().name(),
                        rfq.createdAt())
                .execute();
        rfq.invitedSupplierIds().forEach(supplierId -> dsl.insertInto(table("procurement_rfq_supplier_invitation"))
                .columns(id(), uuid("rfq_id"), uuid("supplier_id"))
                .values(UUID.randomUUID(), rfq.id(), supplierId).execute());
        return rfq;
    }

    @Override
    public Optional<RequestForQuotation> findRfq(final UUID rfqId) {
        return dsl.selectFrom(table("procurement_rfq")).where(id().eq(rfqId)).fetchOptional(this::rfq);
    }

    @Override
    public SupplierQuotation insertQuotation(final SupplierQuotation quotation) {
        dsl.insertInto(table("procurement_supplier_quotation"))
                .columns(id(), text("quotation_number"), text("idempotency_key"), uuid("rfq_id"),
                        uuid("supplier_id"), uuid("currency_id"), uuid("payment_terms_id"),
                        uuid("shipping_method_id"), uuid("incoterms_id"), text("status"), instant("submitted_at"))
                .values(quotation.id(), quotation.quotationNumber(), quotation.idempotencyKey(), quotation.rfqId(),
                        quotation.supplierId(), quotation.currencyId(), quotation.paymentTermsId(),
                        quotation.shippingMethodId(), quotation.incotermsId(), quotation.status().name(),
                        quotation.submittedAt())
                .execute();
        quotation.lines().forEach(line -> insertProcurementLine("procurement_supplier_quotation_line",
                "quotation_id", quotation.id(), line));
        return quotation;
    }

    @Override
    public Optional<SupplierQuotation> findQuotation(final UUID quotationId) {
        return dsl.selectFrom(table("procurement_supplier_quotation")).where(id().eq(quotationId))
                .fetchOptional(this::quotation);
    }

    @Override
    public QuotationComparison insertComparison(final QuotationComparison comparison) {
        dsl.insertInto(table("procurement_quotation_comparison"))
                .columns(id(), uuid("rfq_id"), uuid("selected_quotation_id"), text("compared_quotation_ids"),
                        instant("compared_at"), text("actor"))
                .values(comparison.id(), comparison.rfqId(), comparison.selectedQuotationId(),
                        join(comparison.comparedQuotationIds()), comparison.comparedAt(), comparison.actor())
                .execute();
        return comparison;
    }

    @Override
    public PurchaseOrder insertPurchaseOrder(final PurchaseOrder purchaseOrder) {
        insertOrUpdatePurchaseOrder(purchaseOrder, false);
        return purchaseOrder;
    }

    @Override
    public PurchaseOrder updatePurchaseOrder(final PurchaseOrder purchaseOrder) {
        insertOrUpdatePurchaseOrder(purchaseOrder, true);
        return purchaseOrder;
    }

    @Override
    public Optional<PurchaseOrder> findPurchaseOrder(final UUID purchaseOrderId) {
        return dsl.selectFrom(table("procurement_purchase_order")).where(id().eq(purchaseOrderId))
                .fetchOptional(this::purchaseOrder);
    }

    @Override
    public PurchaseOrderRevision insertRevision(final PurchaseOrderRevision revision) {
        dsl.insertInto(table("procurement_purchase_order_revision"))
                .columns(id(), uuid("purchase_order_id"), integer("revision"), text("reason"),
                        instant("created_at"), text("actor"))
                .values(revision.id(), revision.purchaseOrderId(), revision.revision(), revision.reason(),
                        revision.createdAt(), revision.actor())
                .execute();
        return revision;
    }

    @Override
    public List<PurchaseOrderRevision> listRevisions(final UUID purchaseOrderId) {
        return dsl.selectFrom(table("procurement_purchase_order_revision"))
                .where(uuid("purchase_order_id").eq(purchaseOrderId)).fetch(record -> new PurchaseOrderRevision(
                        record.get(id()), record.get(uuid("purchase_order_id")), record.get(integer("revision")),
                        record.get(text("reason")), instantValue(record, "created_at"), record.get(text("actor"))));
    }

    private void insertOrUpdatePurchaseOrder(final PurchaseOrder order, final boolean update) {
        if (update) {
            dsl.update(table("procurement_purchase_order")).set(text("status"), order.status().name())
                    .set(integer("revision"), order.revision()).where(id().eq(order.id())).execute();
            dsl.deleteFrom(table("procurement_purchase_order_line")).where(uuid("purchase_order_id").eq(order.id()))
                    .execute();
        } else {
            dsl.insertInto(table("procurement_purchase_order"))
                    .columns(id(), text("order_number"), text("idempotency_key"), uuid("requisition_id"),
                            uuid("supplier_id"), uuid("company_id"), uuid("branch_id"), uuid("warehouse_id"),
                            uuid("currency_id"), text("status"), integer("revision"),
                            localDate("expected_delivery_date"), instant("created_at"), text("actor"))
                    .values(order.id(), order.orderNumber(), order.idempotencyKey(), order.requisitionId(),
                            order.supplierId(), order.companyId(), order.branchId(), order.warehouseId(),
                            order.currencyId(), order.status().name(), order.revision(), order.expectedDeliveryDate(),
                            order.createdAt(), order.actor())
                    .execute();
        }
        order.lines().forEach(line -> dsl.insertInto(table("procurement_purchase_order_line"))
                .columns(id(), uuid("purchase_order_id"), uuid("product_id"), uuid("sku_id"), text("sku_code"),
                        decimal("ordered_quantity"), decimal("received_quantity"), decimal("cancelled_quantity"),
                        text("uom_code"), uuid("tax_category_id"))
                .values(line.id(), order.id(), line.productId(), line.skuId(), line.skuCode(),
                        line.orderedQuantity().value(), line.receivedQuantity().value(),
                        line.cancelledQuantity().value(), line.orderedQuantity().uomCode(), line.taxCategoryId())
                .execute());
    }

    private Supplier supplier(final Record record) {
        final UUID supplierId = record.get(id());
        return new Supplier(supplierId, record.get(text("supplier_code")), record.get(text("name")),
                SupplierStatus.valueOf(record.get(text("status"))), contacts(supplierId), addresses(supplierId),
                productReferences(supplierId), instantValue(record, "created_at"));
    }

    private PurchaseRequisition requisition(final Record record) {
        final UUID requisitionId = record.get(id());
        return new PurchaseRequisition(requisitionId, record.get(text("requisition_number")),
                record.get(text("idempotency_key")), record.get(uuid("company_id")), record.get(uuid("branch_id")),
                record.get(uuid("warehouse_id")), RequisitionStatus.valueOf(record.get(text("status"))),
                record.get(integer("revision")), procurementLines("procurement_purchase_requisition_line",
                "requisition_id", requisitionId), instantValue(record, "created_at"), record.get(text("actor")));
    }

    private RequestForQuotation rfq(final Record record) {
        final UUID rfqId = record.get(id());
        final List<UUID> supplierIds = dsl.select(uuid("supplier_id"))
                .from(table("procurement_rfq_supplier_invitation")).where(uuid("rfq_id").eq(rfqId))
                .fetch(uuid("supplier_id"));
        return new RequestForQuotation(rfqId, record.get(text("rfq_number")), record.get(text("idempotency_key")),
                record.get(uuid("requisition_id")), RfqStatus.valueOf(record.get(text("status"))), supplierIds,
                instantValue(record, "created_at"));
    }

    private SupplierQuotation quotation(final Record record) {
        final UUID quotationId = record.get(id());
        return new SupplierQuotation(quotationId, record.get(text("quotation_number")),
                record.get(text("idempotency_key")), record.get(uuid("rfq_id")), record.get(uuid("supplier_id")),
                record.get(uuid("currency_id")), record.get(uuid("payment_terms_id")),
                record.get(uuid("shipping_method_id")), record.get(uuid("incoterms_id")),
                SupplierQuotationStatus.valueOf(record.get(text("status"))),
                procurementLines("procurement_supplier_quotation_line", "quotation_id", quotationId),
                instantValue(record, "submitted_at"));
    }

    private PurchaseOrder purchaseOrder(final Record record) {
        final UUID orderId = record.get(id());
        return new PurchaseOrder(orderId, record.get(text("order_number")), record.get(text("idempotency_key")),
                record.get(uuid("requisition_id")), record.get(uuid("supplier_id")), record.get(uuid("company_id")),
                record.get(uuid("branch_id")), record.get(uuid("warehouse_id")), record.get(uuid("currency_id")),
                PurchaseOrderStatus.valueOf(record.get(text("status"))), record.get(integer("revision")),
                orderLines(orderId), record.get(localDate("expected_delivery_date")),
                instantValue(record, "created_at"), record.get(text("actor")));
    }

    private List<Supplier.SupplierContact> contacts(final UUID supplierId) {
        return dsl.selectFrom(table("procurement_supplier_contact")).where(uuid("supplier_id").eq(supplierId))
                .fetch(record -> new Supplier.SupplierContact(record.get(id()), record.get(text("name")),
                        record.get(text("email")), record.get(text("phone"))));
    }

    private List<Supplier.SupplierAddress> addresses(final UUID supplierId) {
        return dsl.selectFrom(table("procurement_supplier_address")).where(uuid("supplier_id").eq(supplierId))
                .fetch(record -> new Supplier.SupplierAddress(record.get(id()), record.get(uuid("country_id")),
                        record.get(uuid("province_id")), record.get(uuid("city_id")),
                        record.get(text("address_line"))));
    }

    private List<Supplier.SupplierProductReference> productReferences(final UUID supplierId) {
        return dsl.selectFrom(table("procurement_supplier_product_reference"))
                .where(uuid("supplier_id").eq(supplierId))
                .fetch(record -> new Supplier.SupplierProductReference(record.get(id()),
                        record.get(uuid("product_id")), record.get(uuid("sku_id")), record.get(text("supplier_sku")),
                        record.get(integer("lead_time_days")), new ProcurementQuantity(
                        record.get(decimal("minimum_order_quantity")), record.get(text("uom_code"))),
                        record.get(text("packaging_information"))));
    }

    private void insertProcurementLine(final String tableName, final String parentColumn, final UUID parentId,
                                       final ProcurementLine line) {
        dsl.insertInto(table(tableName))
                .columns(id(), uuid(parentColumn), uuid("product_id"), uuid("sku_id"), text("sku_code"),
                        decimal("quantity"), text("uom_code"), decimal("unit_price"), uuid("tax_category_id"))
                .values(line.id(), parentId, line.productId(), line.skuId(), line.skuCode(),
                        line.quantity().value(), line.quantity().uomCode(), line.unitPrice(), line.taxCategoryId())
                .execute();
    }

    private List<ProcurementLine> procurementLines(final String tableName, final String parentColumn,
                                                   final UUID parentId) {
        return dsl.selectFrom(table(tableName)).where(uuid(parentColumn).eq(parentId)).fetch(record ->
                new ProcurementLine(record.get(id()), record.get(uuid("product_id")), record.get(uuid("sku_id")),
                        record.get(text("sku_code")), new ProcurementQuantity(record.get(decimal("quantity")),
                        record.get(text("uom_code"))), record.get(decimal("unit_price")),
                        record.get(uuid("tax_category_id"))));
    }

    private List<PurchaseOrder.PurchaseOrderLine> orderLines(final UUID orderId) {
        return dsl.selectFrom(table("procurement_purchase_order_line"))
                .where(uuid("purchase_order_id").eq(orderId)).fetch(record -> new PurchaseOrder.PurchaseOrderLine(
                        record.get(id()), record.get(uuid("product_id")), record.get(uuid("sku_id")),
                        record.get(text("sku_code")), new ProcurementQuantity(record.get(decimal("ordered_quantity")),
                        record.get(text("uom_code"))), new ProcurementQuantity(
                        record.get(decimal("received_quantity")), record.get(text("uom_code"))),
                        new ProcurementQuantity(record.get(decimal("cancelled_quantity")),
                                record.get(text("uom_code"))), record.get(uuid("tax_category_id"))));
    }

    private static String join(final List<UUID> ids) {
        return String.join(",", ids.stream().map(UUID::toString).toList());
    }

    private static List<UUID> splitIds(final String value) {
        return value == null || value.isBlank() ? List.of() : Arrays.stream(value.split(",")).map(UUID::fromString)
                .toList();
    }

    private static Table<Record> table(final String name) {
        return DSL.table(DSL.name(name));
    }

    private static Field<UUID> id() {
        return uuid("id");
    }

    private static Field<UUID> uuid(final String name) {
        return DSL.field(DSL.name(name), UUID.class);
    }

    private static Field<String> text(final String name) {
        return DSL.field(DSL.name(name), String.class);
    }

    private static Field<Integer> integer(final String name) {
        return DSL.field(DSL.name(name), Integer.class);
    }

    private static Field<BigDecimal> decimal(final String name) {
        return DSL.field(DSL.name(name), BigDecimal.class);
    }

    private static Field<Instant> instant(final String name) {
        return DSL.field(DSL.name(name), Instant.class);
    }

    private static Field<LocalDate> localDate(final String name) {
        return DSL.field(DSL.name(name), LocalDate.class);
    }

    private static Instant instantValue(final Record record, final String name) {
        final Object value = record.get(DSL.field(DSL.name(name)));
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        throw new IllegalStateException("Unsupported timestamp value for " + name + ".");
    }
}
