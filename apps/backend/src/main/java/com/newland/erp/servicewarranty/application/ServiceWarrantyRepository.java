package com.newland.erp.servicewarranty.application;

import com.newland.erp.servicewarranty.domain.ServiceTicket;
import com.newland.erp.servicewarranty.domain.WarrantyPolicy;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface ServiceWarrantyRepository {
  boolean insertTicketIfAbsent(ServiceTicket ticket);

  Optional<ServiceTicket> findTicket(UUID ticketId);

  Optional<ServiceTicket> findTicketByIdempotencyKey(String idempotencyKey);

  ServiceTicket updateTicket(ServiceTicket ticket);

  WarrantyPolicy insertPolicy(WarrantyPolicy policy);

  Optional<WarrantyPolicy> resolvePolicy(UUID companyId, UUID productId, LocalDate effectiveDate);
}
