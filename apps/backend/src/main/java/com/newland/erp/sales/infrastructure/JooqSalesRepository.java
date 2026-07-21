package com.newland.erp.sales.infrastructure;

import com.newland.erp.sales.application.SalesRepository;
import com.newland.erp.sales.domain.Customer;
import com.newland.erp.sales.domain.CustomerStatus;
import com.newland.erp.sales.domain.SalesConflictException;
import com.newland.erp.sales.domain.SalesLine;
import com.newland.erp.sales.domain.SalesOrder;
import com.newland.erp.sales.domain.SalesOrderRevision;
import com.newland.erp.sales.domain.SalesOrderStatus;
import com.newland.erp.sales.domain.SalesQuantity;
import com.newland.erp.sales.domain.SalesQuotation;
import com.newland.erp.sales.domain.SalesQuotationRevision;
import com.newland.erp.sales.domain.SalesQuotationStatus;

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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public final class JooqSalesRepository implements SalesRepository {
    private final DSLContext dsl;

    public JooqSalesRepository(final DSLContext dslContext) {
        this.dsl = dslContext;
    }

    @Override
    public boolean idempotencyKeyExists(final String key) {
        return dsl.fetchExists(table("sales_customer"), text("idempotency_key").eq(key))
                || dsl.fetchExists(table("sales_quotation"), text("idempotency_key").eq(key))
                || dsl.fetchExists(table("sales_order"), text("idempotency_key").eq(key));
    }

    @Override
    public boolean customerCodeExists(final String customerCode) {
        return dsl.fetchExists(table("sales_customer"), text("customer_code").eq(customerCode));
    }

    @Override
    public Customer insertCustomer(final Customer customer) {
        if (dsl.fetchExists(table("sales_customer"), id().eq(customer.id()))) {
            dsl.update(table("sales_customer")).set(text("status"), customer.status().name())
                    .where(id().eq(customer.id())).execute();
        } else {
            dsl.insertInto(table("sales_customer"))
                    .columns(id(), text("idempotency_key"), text("customer_code"), text("name"), text("status"),
                            instant("created_at"))
                    .values(customer.id(), customer.idempotencyKey(), customer.customerCode(), customer.name(),
                            customer.status().name(), customer.createdAt())
                    .execute();
        }
        replaceCustomerChildren(customer);
        return customer;
    }

    @Override
    public Optional<Customer> findCustomer(final UUID customerId) {
        return dsl.selectFrom(table("sales_customer")).where(id().eq(customerId)).fetchOptional(this::customer);
    }

    @Override
    public List<Customer> listCustomers() {
        return dsl.selectFrom(table("sales_customer")).orderBy(text("customer_code")).fetch(this::customer);
    }

    @Override
    public SalesQuotation insertQuotation(final SalesQuotation quotation) {
        insertOrUpdateQuotation(quotation, false);
        return quotation;
    }

    @Override
    public SalesQuotation updateQuotation(final SalesQuotation quotation) {
        insertOrUpdateQuotation(quotation, true);
        return quotation;
    }

    @Override
    public Optional<SalesQuotation> findQuotation(final UUID quotationId) {
        return dsl.selectFrom(table("sales_quotation")).where(id().eq(quotationId)).fetchOptional(this::quotation);
    }

    @Override
    public SalesQuotationRevision insertQuotationRevision(final SalesQuotationRevision revision) {
        dsl.insertInto(table("sales_quotation_revision"))
                .columns(id(), uuid("quotation_id"), integer("revision"), text("reason"), instant("created_at"),
                        text("actor"))
                .values(revision.id(), revision.quotationId(), revision.revision(), revision.reason(),
                        revision.createdAt(), revision.actor())
                .execute();
        return revision;
    }

    @Override
    public List<SalesQuotationRevision> listQuotationRevisions(final UUID quotationId) {
        return dsl.selectFrom(table("sales_quotation_revision")).where(uuid("quotation_id").eq(quotationId))
                .fetch(record -> new SalesQuotationRevision(record.get(id()), record.get(uuid("quotation_id")),
                        record.get(integer("revision")), record.get(text("reason")),
                        instantValue(record, "created_at"), record.get(text("actor"))));
    }

    @Override
    public SalesOrder insertSalesOrder(final SalesOrder order) {
        insertOrUpdateOrder(order, false);
        return order;
    }

    @Override
    public SalesOrder updateSalesOrder(final SalesOrder order) {
        insertOrUpdateOrder(order, true);
        return order;
    }

    @Override
    public Optional<SalesOrder> findSalesOrder(final UUID orderId) {
        return dsl.selectFrom(table("sales_order")).where(id().eq(orderId)).fetchOptional(this::salesOrder);
    }

    @Override
    public SalesOrderRevision insertOrderRevision(final SalesOrderRevision revision) {
        dsl.insertInto(table("sales_order_revision"))
                .columns(id(), uuid("sales_order_id"), integer("revision"), text("reason"), instant("created_at"),
                        text("actor"))
                .values(revision.id(), revision.salesOrderId(), revision.revision(), revision.reason(),
                        revision.createdAt(), revision.actor())
                .execute();
        return revision;
    }

    @Override
    public List<SalesOrderRevision> listOrderRevisions(final UUID orderId) {
        return dsl.selectFrom(table("sales_order_revision")).where(uuid("sales_order_id").eq(orderId))
                .fetch(record -> new SalesOrderRevision(record.get(id()), record.get(uuid("sales_order_id")),
                        record.get(integer("revision")), record.get(text("reason")),
                        instantValue(record, "created_at"), record.get(text("actor"))));
    }

    private void insertOrUpdateQuotation(final SalesQuotation quotation, final boolean update) {
        if (update) {
            final int updated = dsl.update(table("sales_quotation")).set(text("status"), quotation.status().name())
                    .set(integer("revision"), quotation.revision())
                    .set(integer("lock_version"), quotation.lockVersion() + 1)
                    .where(id().eq(quotation.id()).and(integer("lock_version").eq(quotation.lockVersion())))
                    .execute();
            if (updated != 1) {
                throw new SalesConflictException("Sales quotation was modified concurrently.");
            }
            dsl.deleteFrom(table("sales_quotation_line")).where(uuid("quotation_id").eq(quotation.id())).execute();
        } else {
            dsl.insertInto(table("sales_quotation"))
                    .columns(id(), text("quotation_number"), text("idempotency_key"), uuid("customer_id"),
                            uuid("company_id"), uuid("branch_id"), uuid("warehouse_id"), uuid("sales_channel_id"),
                            uuid("currency_id"), uuid("payment_terms_id"), uuid("shipping_method_id"),
                            uuid("incoterms_id"), text("status"), integer("revision"), integer("lock_version"),
                            localDate("expires_on"), instant("created_at"), text("actor"))
                    .values(quotation.id(), quotation.quotationNumber(), quotation.idempotencyKey(),
                            quotation.customerId(), quotation.companyId(), quotation.branchId(),
                            quotation.warehouseId(), quotation.salesChannelId(), quotation.currencyId(),
                            quotation.paymentTermsId(), quotation.shippingMethodId(), quotation.incotermsId(),
                            quotation.status().name(), quotation.revision(), quotation.lockVersion(),
                            quotation.expiresOn(),
                            quotation.createdAt(), quotation.actor())
                    .execute();
        }
        quotation.lines().forEach(line -> insertLine("sales_quotation_line", "quotation_id", quotation.id(), line));
    }

    private void insertOrUpdateOrder(final SalesOrder order, final boolean update) {
        if (update) {
            final int updated = dsl.update(table("sales_order")).set(text("status"), order.status().name())
                    .set(integer("revision"), order.revision()).set(integer("lock_version"), order.lockVersion() + 1)
                    .where(id().eq(order.id()).and(integer("lock_version").eq(order.lockVersion()))).execute();
            if (updated != 1) {
                throw new SalesConflictException("Sales order was modified concurrently.");
            }
            dsl.deleteFrom(table("sales_order_line")).where(uuid("sales_order_id").eq(order.id())).execute();
        } else {
            dsl.insertInto(table("sales_order"))
                    .columns(id(), text("order_number"), text("idempotency_key"), uuid("quotation_id"),
                            uuid("customer_id"), uuid("company_id"), uuid("branch_id"), uuid("warehouse_id"),
                            uuid("sales_channel_id"), uuid("currency_id"), text("status"), integer("revision"),
                            integer("lock_version"), localDate("requested_delivery_date"), instant("created_at"),
                            text("actor"))
                    .values(order.id(), order.orderNumber(), order.idempotencyKey(), order.quotationId(),
                            order.customerId(), order.companyId(), order.branchId(), order.warehouseId(),
                            order.salesChannelId(), order.currencyId(), order.status().name(), order.revision(),
                            order.lockVersion(), order.requestedDeliveryDate(), order.createdAt(), order.actor())
                    .execute();
        }
        order.lines().forEach(line -> dsl.insertInto(table("sales_order_line"))
                .columns(id(), uuid("sales_order_id"), uuid("product_id"), uuid("sku_id"), text("sku_code"),
                        decimal("ordered_quantity"), decimal("reserved_quantity"), decimal("delivered_quantity"),
                        decimal("cancelled_quantity"), text("uom_code"), uuid("tax_category_id"))
                .values(line.id(), order.id(), line.productId(), line.skuId(), line.skuCode(),
                        line.orderedQuantity().value(), line.reservedQuantity().value(),
                        line.deliveredQuantity().value(), line.cancelledQuantity().value(),
                        line.orderedQuantity().uomCode(), line.taxCategoryId())
                .execute());
    }

    private Customer customer(final Record record) {
        final UUID customerId = record.get(id());
        return new Customer(record.get(id()), record.get(text("idempotency_key")), record.get(text("customer_code")),
                record.get(text("name")), CustomerStatus.valueOf(record.get(text("status"))),
                customerContacts(customerId), customerAddresses(customerId), customerCreditProfiles(customerId),
                customerProductReferences(customerId), instantValue(record, "created_at"));
    }

    private SalesQuotation quotation(final Record record) {
        final UUID quotationId = record.get(id());
        return new SalesQuotation(quotationId, record.get(text("quotation_number")),
                record.get(text("idempotency_key")), record.get(uuid("customer_id")), record.get(uuid("company_id")),
                record.get(uuid("branch_id")), record.get(uuid("warehouse_id")),
                record.get(uuid("sales_channel_id")), record.get(uuid("currency_id")),
                record.get(uuid("payment_terms_id")), record.get(uuid("shipping_method_id")),
                record.get(uuid("incoterms_id")), SalesQuotationStatus.valueOf(record.get(text("status"))),
                record.get(integer("revision")), lines("sales_quotation_line", "quotation_id", quotationId),
                record.get(integer("lock_version")), record.get(localDate("expires_on")),
                instantValue(record, "created_at"), record.get(text("actor")));
    }

    private SalesOrder salesOrder(final Record record) {
        final UUID orderId = record.get(id());
        return new SalesOrder(orderId, record.get(text("order_number")), record.get(text("idempotency_key")),
                record.get(uuid("quotation_id")), record.get(uuid("customer_id")), record.get(uuid("company_id")),
                record.get(uuid("branch_id")), record.get(uuid("warehouse_id")),
                record.get(uuid("sales_channel_id")), record.get(uuid("currency_id")),
                SalesOrderStatus.valueOf(record.get(text("status"))), record.get(integer("revision")),
                orderLines(orderId), record.get(integer("lock_version")),
                record.get(localDate("requested_delivery_date")), instantValue(record, "created_at"),
                record.get(text("actor")));
    }

    private void insertLine(final String tableName, final String parentColumn, final UUID parentId,
                            final SalesLine line) {
        dsl.insertInto(table(tableName))
                .columns(id(), uuid(parentColumn), uuid("product_id"), uuid("sku_id"), text("sku_code"),
                        decimal("quantity"), text("uom_code"), decimal("unit_price"), uuid("tax_category_id"))
                .values(line.id(), parentId, line.productId(), line.skuId(), line.skuCode(), line.quantity().value(),
                        line.quantity().uomCode(), line.unitPrice(), line.taxCategoryId())
                .execute();
    }

    private List<SalesLine> lines(final String tableName, final String parentColumn, final UUID parentId) {
        return dsl.selectFrom(table(tableName)).where(uuid(parentColumn).eq(parentId)).fetch(record ->
                new SalesLine(record.get(id()), record.get(uuid("product_id")), record.get(uuid("sku_id")),
                        record.get(text("sku_code")), new SalesQuantity(record.get(decimal("quantity")),
                        record.get(text("uom_code"))), record.get(decimal("unit_price")),
                        record.get(uuid("tax_category_id"))));
    }

    private void replaceCustomerChildren(final Customer customer) {
        dsl.deleteFrom(table("sales_customer_contact")).where(uuid("customer_id").eq(customer.id())).execute();
        dsl.deleteFrom(table("sales_customer_address")).where(uuid("customer_id").eq(customer.id())).execute();
        dsl.deleteFrom(table("sales_customer_credit_profile")).where(uuid("customer_id").eq(customer.id())).execute();
        dsl.deleteFrom(table("sales_customer_product_reference")).where(uuid("customer_id").eq(customer.id()))
                .execute();
        customer.contacts().forEach(contact -> dsl.insertInto(table("sales_customer_contact"))
                .columns(id(), uuid("customer_id"), text("name"), text("email"), text("phone"))
                .values(contact.id(), customer.id(), contact.name(), contact.email(), contact.phone()).execute());
        customer.addresses().forEach(address -> dsl.insertInto(table("sales_customer_address"))
                .columns(id(), uuid("customer_id"), uuid("country_id"), uuid("province_id"), uuid("city_id"),
                        text("address_line"))
                .values(address.id(), customer.id(), address.countryId(), address.provinceId(), address.cityId(),
                        address.addressLine()).execute());
        customer.creditProfiles().forEach(profile -> dsl.insertInto(table("sales_customer_credit_profile"))
                .columns(id(), uuid("customer_id"), uuid("company_id"), uuid("currency_id"), decimal("credit_limit"),
                        DSL.field(DSL.name("credit_hold"), Boolean.class))
                .values(profile.id(), customer.id(), profile.companyId(), profile.currencyId(),
                        profile.creditLimit(), profile.creditHold()).execute());
        customer.productReferences().forEach(reference -> dsl.insertInto(table("sales_customer_product_reference"))
                .columns(id(), uuid("customer_id"), uuid("product_id"), uuid("sku_id"), text("customer_sku"))
                .values(reference.id(), customer.id(), reference.productId(), reference.skuId(),
                        reference.customerSku()).execute());
    }

    private List<Customer.CustomerContact> customerContacts(final UUID customerId) {
        return dsl.selectFrom(table("sales_customer_contact")).where(uuid("customer_id").eq(customerId))
                .fetch(record -> new Customer.CustomerContact(record.get(id()), record.get(text("name")),
                        record.get(text("email")), record.get(text("phone"))));
    }

    private List<Customer.CustomerAddress> customerAddresses(final UUID customerId) {
        return dsl.selectFrom(table("sales_customer_address")).where(uuid("customer_id").eq(customerId))
                .fetch(record -> new Customer.CustomerAddress(record.get(id()), record.get(uuid("country_id")),
                        record.get(uuid("province_id")), record.get(uuid("city_id")),
                        record.get(text("address_line"))));
    }

    private List<Customer.CustomerCreditProfile> customerCreditProfiles(final UUID customerId) {
        return dsl.selectFrom(table("sales_customer_credit_profile")).where(uuid("customer_id").eq(customerId))
                .fetch(record -> new Customer.CustomerCreditProfile(record.get(id()), record.get(uuid("company_id")),
                        record.get(uuid("currency_id")), record.get(decimal("credit_limit")),
                        record.get(DSL.field(DSL.name("credit_hold"), Boolean.class))));
    }

    private List<Customer.CustomerProductReference> customerProductReferences(final UUID customerId) {
        return dsl.selectFrom(table("sales_customer_product_reference")).where(uuid("customer_id").eq(customerId))
                .fetch(record -> new Customer.CustomerProductReference(record.get(id()),
                        record.get(uuid("product_id")), record.get(uuid("sku_id")),
                        record.get(text("customer_sku"))));
    }

    private List<SalesOrder.SalesOrderLine> orderLines(final UUID orderId) {
        return dsl.selectFrom(table("sales_order_line")).where(uuid("sales_order_id").eq(orderId)).fetch(record ->
                new SalesOrder.SalesOrderLine(record.get(id()), record.get(uuid("product_id")),
                        record.get(uuid("sku_id")), record.get(text("sku_code")),
                        new SalesQuantity(record.get(decimal("ordered_quantity")), record.get(text("uom_code"))),
                        new SalesQuantity(record.get(decimal("reserved_quantity")), record.get(text("uom_code"))),
                        new SalesQuantity(record.get(decimal("delivered_quantity")), record.get(text("uom_code"))),
                        new SalesQuantity(record.get(decimal("cancelled_quantity")), record.get(text("uom_code"))),
                        record.get(uuid("tax_category_id"))));
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
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        return null;
    }
}
