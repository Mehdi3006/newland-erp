package com.newland.erp.crm.application;

import com.newland.erp.crm.domain.Activity;
import com.newland.erp.crm.domain.Lead;
import com.newland.erp.crm.domain.Opportunity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CrmRepository {
  boolean insertLeadIfAbsent(Lead lead);

  Optional<Lead> findLead(UUID leadId);

  Optional<Lead> findLeadByIdempotencyKey(String idempotencyKey);

  Lead updateLead(Lead lead);

  boolean insertOpportunityIfAbsent(Opportunity opportunity);

  Optional<Opportunity> findOpportunity(UUID opportunityId);

  Optional<Opportunity> findOpportunityByIdempotencyKey(String idempotencyKey);

  Opportunity updateOpportunity(Opportunity opportunity);

  boolean insertActivityIfAbsent(Activity activity);

  Optional<Activity> findActivityByIdempotencyKey(String idempotencyKey);

  List<Activity> listCustomerActivities(UUID companyId, UUID customerId);
}
