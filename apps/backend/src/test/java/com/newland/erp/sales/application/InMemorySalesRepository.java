package com.newland.erp.sales.application;

import com.newland.erp.sales.domain.Customer;
import com.newland.erp.sales.domain.SalesOrder;
import com.newland.erp.sales.domain.SalesOrderRevision;
import com.newland.erp.sales.domain.SalesQuotation;
import com.newland.erp.sales.domain.SalesQuotationRevision;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class InMemorySalesRepository implements SalesRepository {
    final List<Customer> customers = new ArrayList<>();
    final List<SalesQuotation> quotations = new ArrayList<>();
    final List<SalesQuotationRevision> quotationRevisions = new ArrayList<>();
    final List<SalesOrder> orders = new ArrayList<>();
    final List<SalesOrderRevision> orderRevisions = new ArrayList<>();

    @Override
    public boolean idempotencyKeyExists(final String idempotencyKey) {
        return customers.stream().anyMatch(item -> item.idempotencyKey().equals(idempotencyKey))
                || quotations.stream().anyMatch(item -> item.idempotencyKey().equals(idempotencyKey))
                || orders.stream().anyMatch(item -> item.idempotencyKey().equals(idempotencyKey));
    }

    @Override
    public boolean customerCodeExists(final String customerCode) {
        return customers.stream().anyMatch(item -> item.customerCode().equals(customerCode));
    }

    @Override
    public Customer insertCustomer(final Customer customer) {
        customers.removeIf(item -> item.id().equals(customer.id()));
        customers.add(customer);
        return customer;
    }

    @Override
    public Optional<Customer> findCustomer(final UUID customerId) {
        return customers.stream().filter(item -> item.id().equals(customerId)).findFirst();
    }

    @Override
    public List<Customer> listCustomers() {
        return List.copyOf(customers);
    }

    @Override
    public SalesQuotation insertQuotation(final SalesQuotation quotation) {
        quotations.add(quotation);
        return quotation;
    }

    @Override
    public SalesQuotation updateQuotation(final SalesQuotation quotation) {
        quotations.removeIf(item -> item.id().equals(quotation.id()));
        quotations.add(quotation);
        return quotation;
    }

    @Override
    public Optional<SalesQuotation> findQuotation(final UUID quotationId) {
        return quotations.stream().filter(item -> item.id().equals(quotationId)).findFirst();
    }

    @Override
    public SalesQuotationRevision insertQuotationRevision(final SalesQuotationRevision revision) {
        quotationRevisions.add(revision);
        return revision;
    }

    @Override
    public List<SalesQuotationRevision> listQuotationRevisions(final UUID quotationId) {
        return quotationRevisions.stream().filter(item -> item.quotationId().equals(quotationId)).toList();
    }

    @Override
    public SalesOrder insertSalesOrder(final SalesOrder order) {
        orders.add(order);
        return order;
    }

    @Override
    public SalesOrder updateSalesOrder(final SalesOrder order) {
        orders.removeIf(item -> item.id().equals(order.id()));
        orders.add(order);
        return order;
    }

    @Override
    public Optional<SalesOrder> findSalesOrder(final UUID orderId) {
        return orders.stream().filter(item -> item.id().equals(orderId)).findFirst();
    }

    @Override
    public SalesOrderRevision insertOrderRevision(final SalesOrderRevision revision) {
        orderRevisions.add(revision);
        return revision;
    }

    @Override
    public List<SalesOrderRevision> listOrderRevisions(final UUID orderId) {
        return orderRevisions.stream().filter(item -> item.salesOrderId().equals(orderId)).toList();
    }
}
