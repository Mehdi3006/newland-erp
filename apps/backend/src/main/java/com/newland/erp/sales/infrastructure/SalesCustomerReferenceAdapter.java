package com.newland.erp.sales.infrastructure;

import com.newland.erp.sales.application.SalesRepository;
import com.newland.erp.sales.application.integration.SalesCustomerReferencePort;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public final class SalesCustomerReferenceAdapter implements SalesCustomerReferencePort {
  private final SalesRepository repository;

  public SalesCustomerReferenceAdapter(final SalesRepository salesRepository) {
    repository = salesRepository;
  }

  @Override
  public CustomerReference requireCustomer(final UUID customerId, final UUID companyId) {
    final var customer =
        repository
            .findCustomer(customerId)
            .orElseThrow(() -> new IllegalArgumentException("Sales customer not found."));
    final boolean companyScoped =
        customer.creditProfiles().stream()
            .anyMatch(profile -> profile.companyId().equals(companyId));
    if (!companyScoped) {
      throw new IllegalArgumentException("Sales customer is outside CRM company scope.");
    }
    return new CustomerReference(customer.id(), customer.customerCode(), customer.name());
  }
}
