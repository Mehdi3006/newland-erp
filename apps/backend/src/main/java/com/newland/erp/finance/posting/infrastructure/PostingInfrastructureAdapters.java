package com.newland.erp.finance.posting.infrastructure;

import com.newland.erp.finance.application.FinanceRepository;
import com.newland.erp.finance.application.FinanceService;
import com.newland.erp.finance.posting.application.PostingPorts;
import com.newland.erp.finance.posting.application.PostingRuleEvaluator;
import com.newland.erp.finance.domain.JournalEntry;
import com.newland.erp.finance.posting.domain.AccountingEvent;
import com.newland.erp.finance.posting.domain.PostingException;
import com.newland.erp.finance.posting.domain.PostingRule;
import com.newland.erp.identity.application.IdentityService;
import com.newland.erp.platform.application.PlatformCommands;
import com.newland.erp.platform.application.PlatformService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Component;

public final class PostingInfrastructureAdapters {
  @Component
  public static final class CompanyAdapter implements PostingPorts.CompanyValidationPort {
    private final DSLContext dsl;

    public CompanyAdapter(final DSLContext dslContext) {
      dsl = dslContext;
    }

    public void requireCompany(final UUID id) {
      require(id, "company");
      final boolean active =
          dsl.fetchExists(
              DSL.table("company"),
              DSL.field("id", UUID.class)
                  .eq(id)
                  .and(DSL.field("status", String.class).eq("ACTIVE")));
      if (!active) {
        throw new PostingException("Posting company is missing or inactive.");
      }
    }
  }

  @Component
  public static final class BranchAdapter implements PostingPorts.BranchValidationPort {
    private final DSLContext dsl;

    public BranchAdapter(final DSLContext dslContext) {
      dsl = dslContext;
    }

    public void requireBranch(final UUID company, final UUID branch) {
      require(company, "company");
      require(branch, "branch");
      final boolean active =
          dsl.fetchExists(
              DSL.table("branch"),
              DSL.field("id", UUID.class)
                  .eq(branch)
                  .and(DSL.field("company_id", UUID.class).eq(company))
                  .and(DSL.field("status", String.class).eq("ACTIVE")));
      if (!active) {
        throw new PostingException("Posting branch is missing, inactive, or outside company scope.");
      }
    }
  }

  @Component
  public static final class CurrencyAdapter implements PostingPorts.CurrencyValidationPort {
    private final DSLContext dsl;

    public CurrencyAdapter(final DSLContext dslContext) {
      dsl = dslContext;
    }

    public void requireCurrency(final String code) {
      if (code == null || code.isBlank()) {
        throw new PostingException("Currency is required.");
      }
      final boolean active =
          dsl.fetchExists(
              DSL.table("master_data_record"),
              DSL.field("aggregate_type", String.class)
                  .eq("CURRENCY")
                  .and(DSL.field("code", String.class).eq(code.trim().toUpperCase()))
                  .and(DSL.field("active", Boolean.class).eq(true)));
      if (!active) {
        throw new PostingException("Posting currency is missing or inactive.");
      }
    }
  }

  @Component
  public static final class RateAdapter implements PostingPorts.ExchangeRateValidationPort {
    public void requireRate(final String code, final BigDecimal rate, final LocalDate date) {
      if (rate == null || rate.signum() <= 0) {
        throw new IllegalArgumentException("Exchange rate is required.");
      }
    }
  }

  @Component
  public static final class PeriodAdapter implements PostingPorts.AccountingPeriodPort {
    private final FinanceRepository finance;

    public PeriodAdapter(final FinanceRepository financeRepository) {
      finance = financeRepository;
    }

    public void requireOpenPeriod(final UUID company, final LocalDate date) {
      require(company, "company");
      if (date == null) {
        throw new PostingException("Accounting date is required.");
      }
      if (finance.findOpenPostingPeriod(company, date).isEmpty()) {
        throw new PostingException("No open accounting period exists for the posting date.");
      }
    }
  }

  @Component
  public static final class FinanceReferenceAdapter
      implements PostingPorts.AccountResolutionPort,
          PostingPorts.CostCenterValidationPort,
          PostingPorts.ProfitCenterValidationPort {
    private final FinanceRepository finance;

    public FinanceReferenceAdapter(final FinanceRepository financeRepository) {
      finance = financeRepository;
    }

    @Override
    public void requireAccount(final UUID companyId, final UUID accountId) {
      final var account =
          finance
              .findAccount(companyId, accountId)
              .orElseThrow(
                  () -> new PostingException("Account is not valid for the posting company."));
      account.requirePostable();
    }

    @Override
    public UUID resolveAttribute(
        final UUID companyId, final String key, final Map<String, String> attributes) {
      final String value = attributes.get(key);
      if (value == null || value.isBlank()) {
        throw new PostingException("Required event attribute is missing: " + key);
      }
      final UUID accountId = identifier(value, "account attribute " + key);
      requireAccount(companyId, accountId);
      return accountId;
    }

    @Override
    public void requireCostCenter(final UUID companyId, final UUID costCenterId) {
      final var costCenter =
          finance
              .findCostCenter(companyId, costCenterId)
              .orElseThrow(
                  () ->
                      new PostingException(
                          "Cost center is not valid for the posting company."));
      if (!costCenter.active()) {
        throw new PostingException("Posting to an inactive cost center is forbidden.");
      }
    }

    @Override
    public void requireProfitCenter(final UUID companyId, final UUID profitCenterId) {
      final var profitCenter =
          finance
              .findProfitCenter(companyId, profitCenterId)
              .orElseThrow(
                  () ->
                      new PostingException(
                          "Profit center is not valid for the posting company."));
      if (!profitCenter.active()) {
        throw new PostingException("Posting to an inactive profit center is forbidden.");
      }
    }
  }

  @Component
  public static final class DimensionsAdapter
      implements PostingPorts.FinancialDimensionValidationPort {
    private final FinanceRepository finance;

    public DimensionsAdapter(final FinanceRepository financeRepository) {
      finance = financeRepository;
    }

    public void requireDimensions(final UUID company, final Map<String, String> dimensions) {
      require(company, "company");
      dimensions.forEach(
          (key, value) -> {
            if (key == null || key.isBlank() || value == null || value.isBlank()) {
              throw new PostingException("Financial dimension names and values are required.");
            }
          });
    }

    public void requireDimension(final UUID company, final String dimensionCode) {
      require(company, "company");
      if (dimensionCode == null || dimensionCode.isBlank()) {
        throw new PostingException("Financial dimension is required.");
      }
      if (!finance.financialDimensionIsActive(company, dimensionCode)) {
        throw new PostingException(
            "Financial dimension is missing, inactive, or outside company scope.");
      }
    }
  }

  @Component
  public static final class JournalAdapter implements PostingPorts.JournalPostingPort {
    private final FinanceService finance;
    private final FinanceRepository repository;
    private final PostingRuleEvaluator evaluator;

    public JournalAdapter(
        final FinanceService financeService,
        final FinanceRepository financeRepository,
        final PostingRuleEvaluator postingRuleEvaluator) {
      finance = financeService;
      repository = financeRepository;
      evaluator = postingRuleEvaluator;
    }

    public PostingPorts.JournalReference createAndPost(
        final AccountingEvent event, final PostingRule rule) {
      final List<JournalEntry.JournalLine> lines = evaluator.evaluate(event, rule);
      final JournalEntry journal =
          finance.createAndPostJournal(
              "posting:" + event.eventId(),
              event.companyId(),
              event.branchId(),
              event.accountingDate(),
              lines,
              event.submittedBy());
      return new PostingPorts.JournalReference(journal.id(), journal.number());
    }

    @Override
    public PostingPorts.JournalReference findReference(final UUID journalEntryId) {
      final JournalEntry journal =
          repository
              .findJournal(journalEntryId)
              .orElseThrow(() -> new PostingException("Posted Finance journal not found."));
      if (journal.status() != JournalEntry.JournalStatus.POSTED) {
        throw new PostingException("Finance journal is not posted.");
      }
      return new PostingPorts.JournalReference(journal.id(), journal.number());
    }
  }

  @Component
  public static final class PlatformAdapter
      implements PostingPorts.AuditPort, PostingPorts.TransactionalOutboxPort {
    private final PlatformService platform;

    public PlatformAdapter(final PlatformService platformService) {
      platform = platformService;
    }

    public void record(final String actor, final String event, final UUID id) {
      platform.recordAudit(
          new PlatformCommands.RecordAudit(
              actor, event, "FinancePosting", id, Map.of()));
    }

    public void publish(final String event, final UUID id) {
      platform.publishEvent(
          new PlatformCommands.PublishEvent("finance-posting", event, id, Map.of()));
    }
  }

  @Component
  public static final class SecurityAdapter
      implements PostingPorts.CurrentUserPort, PostingPorts.AuthorizationPort {
    private final IdentityService identity;

    public SecurityAdapter(final IdentityService identityService) {
      identity = identityService;
    }

    public String currentUser() {
      final var authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication == null || !authentication.isAuthenticated()) {
        throw new AuthenticationCredentialsNotFoundException("Authentication is required.");
      }
      return authentication.getName();
    }

    public void require(final String actor, final String capability, final UUID companyId) {
      final var decision =
          identity.decideCompany(authenticatedUser(actor), capability, companyId);
      if (!decision.granted()) {
        throw new AccessDeniedException("Permission denied for company scope.");
      }
    }

    public void requireGlobal(final String actor, final String capability) {
      if (!identity.hasEnterpriseCapability(authenticatedUser(actor), capability)) {
        throw new AccessDeniedException("Permission denied for enterprise scope.");
      }
    }

    private UUID authenticatedUser(final String actor) {
      try {
        return UUID.fromString(actor);
      } catch (IllegalArgumentException | NullPointerException exception) {
        throw new AuthenticationCredentialsNotFoundException(
            "Authenticated user identifier is invalid.");
      }
    }
  }

  private static void require(final UUID id, final String name) {
    if (id == null) {
      throw new IllegalArgumentException(name + " is required.");
    }
  }

  private static UUID identifier(final String value, final String label) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException exception) {
      throw new PostingException("Invalid " + label + ".");
    }
  }

  private PostingInfrastructureAdapters() {}
}
