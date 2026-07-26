package com.newland.erp.sales.application.integration;

import java.util.UUID;

public interface SalesCustomerReferencePort {
  CustomerReference requireCustomer(UUID customerId, UUID companyId);

  record CustomerReference(UUID customerId, String customerCode, String name) {}
}
