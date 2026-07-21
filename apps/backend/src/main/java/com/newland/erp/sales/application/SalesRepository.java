package com.newland.erp.sales.application;

import com.newland.erp.sales.domain.Customer;
import com.newland.erp.sales.domain.SalesOrder;
import com.newland.erp.sales.domain.SalesOrderRevision;
import com.newland.erp.sales.domain.SalesQuotation;
import com.newland.erp.sales.domain.SalesQuotationRevision;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SalesRepository {
    boolean idempotencyKeyExists(String idempotencyKey);

    boolean customerCodeExists(String customerCode);

    Customer insertCustomer(Customer customer);

    Optional<Customer> findCustomer(UUID customerId);

    List<Customer> listCustomers();

    SalesQuotation insertQuotation(SalesQuotation quotation);

    SalesQuotation updateQuotation(SalesQuotation quotation);

    Optional<SalesQuotation> findQuotation(UUID quotationId);

    SalesQuotationRevision insertQuotationRevision(SalesQuotationRevision revision);

    List<SalesQuotationRevision> listQuotationRevisions(UUID quotationId);

    SalesOrder insertSalesOrder(SalesOrder order);

    SalesOrder updateSalesOrder(SalesOrder order);

    Optional<SalesOrder> findSalesOrder(UUID orderId);

    SalesOrderRevision insertOrderRevision(SalesOrderRevision revision);

    List<SalesOrderRevision> listOrderRevisions(UUID orderId);
}
