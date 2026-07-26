package com.newland.erp.finance.posting.application;

import com.newland.erp.finance.posting.domain.AccountingEvent;
import com.newland.erp.finance.posting.domain.PostingRequest;
import com.newland.erp.finance.posting.domain.PostingRule;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PostingPorts {
  public interface EventRepository {
    boolean eventIdExists(UUID eventId);

    java.util.Optional<AccountingEvent> findEvent(UUID eventId);

    java.util.Optional<AccountingEvent> findByIdempotencyKey(String key);

    AccountingEvent saveEvent(AccountingEvent event);

    PostingRequest saveRequest(PostingRequest request);

    java.util.Optional<PostingRequest> findRequest(UUID requestId);

    java.util.Optional<PostingRequest> findRequestByEvent(UUID eventId);

    PostingRequest updateRequest(PostingRequest request);

    java.util.Optional<PostingRequest> claimRequest(UUID requestId, int expectedVersion);

    void lockEvent(UUID eventId);
  }

  public interface RuleRepository {
    List<PostingRule> findApplicable(String eventType, UUID companyId, LocalDate date);

    PostingRule save(PostingRule rule);

    PostingRule transition(PostingRule rule, PostingRule.Status expectedStatus);

    java.util.Optional<PostingRule> findRule(UUID postingRuleId);

    java.util.Optional<PostingRule> findLatest(String code, UUID companyId);

    void lockActivationScope(PostingRule candidate);

    List<PostingRule> findActivationConflicts(PostingRule candidate);

    List<PostingRule> list();

    List<PostingRule> list(UUID companyId);
  }

  public interface CompanyValidationPort {
    void requireCompany(UUID companyId);
  }

  public interface BranchValidationPort {
    void requireBranch(UUID companyId, UUID branchId);
  }

  public interface CurrencyValidationPort {
    void requireCurrency(String currencyCode);
  }

  public interface ExchangeRateValidationPort {
    void requireRate(String currencyCode, BigDecimal rate, LocalDate date);
  }

  public interface AccountingPeriodPort {
    void requireOpenPeriod(UUID companyId, LocalDate date);
  }

  public interface AccountResolutionPort {
    void requireAccount(UUID companyId, UUID accountId);

    UUID resolveAttribute(UUID companyId, String key, Map<String, String> attributes);
  }

  public interface CostCenterValidationPort {
    void requireCostCenter(UUID companyId, UUID costCenterId);
  }

  public interface ProfitCenterValidationPort {
    void requireProfitCenter(UUID companyId, UUID profitCenterId);
  }

  public interface FinancialDimensionValidationPort {
    void requireDimensions(UUID companyId, Map<String, String> dimensions);

    void requireDimension(UUID companyId, String dimensionCode);
  }

  public interface JournalPostingPort {
    JournalReference createAndPost(AccountingEvent event, PostingRule rule);

    JournalReference findReference(UUID journalEntryId);
  }

  public record JournalReference(UUID journalEntryId, String journalNumber) {}

  public interface AuditPort {
    void record(String actor, String eventType, UUID aggregateId);
  }

  public interface TransactionalOutboxPort {
    void publish(String eventType, UUID aggregateId);
  }

  public interface CurrentUserPort {
    String currentUser();
  }

  public interface AuthorizationPort {
    void require(String actor, String capability, UUID companyId);

    void requireGlobal(String actor, String capability);
  }

  private PostingPorts() {}
}
