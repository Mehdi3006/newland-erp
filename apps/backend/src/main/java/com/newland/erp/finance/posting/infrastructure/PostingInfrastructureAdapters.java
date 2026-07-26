package com.newland.erp.finance.posting.infrastructure;

import com.newland.erp.finance.application.FinanceRepository;
import com.newland.erp.finance.application.FinanceService;
import com.newland.erp.finance.posting.application.PostingPorts;
import com.newland.erp.finance.posting.application.PostingRuleEvaluator;
import com.newland.erp.finance.domain.JournalEntry;
import com.newland.erp.finance.domain.JournalPostingSnapshot;
import com.newland.erp.finance.posting.domain.AccountingEvent;
import com.newland.erp.finance.posting.domain.PostingException;
import com.newland.erp.finance.posting.domain.PostingRule;
import com.newland.erp.enterprise.application.integration.EnterpriseReferencePort;
import com.newland.erp.identity.application.integration.IdentityAuthorizationPort;
import com.newland.erp.masterdata.application.integration.MasterDataReferencePort;
import com.newland.erp.platform.application.integration.PlatformAuditOutboxPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Component;

public final class PostingInfrastructureAdapters {
  @Component
  public static final class CompanyAdapter implements PostingPorts.CompanyValidationPort {
    private final EnterpriseReferencePort enterprise;

    public CompanyAdapter(final EnterpriseReferencePort enterpriseReferencePort) {
      enterprise = enterpriseReferencePort;
    }

    public void requireCompany(final UUID id) {
      require(id, "company");
      if (!enterprise.isActiveCompany(id)) {
        throw new PostingException("Posting company is missing or inactive.");
      }
    }
  }

  @Component
  public static final class BranchAdapter implements PostingPorts.BranchValidationPort {
    private final EnterpriseReferencePort enterprise;

    public BranchAdapter(final EnterpriseReferencePort enterpriseReferencePort) {
      enterprise = enterpriseReferencePort;
    }

    public void requireBranch(final UUID company, final UUID branch) {
      require(company, "company");
      require(branch, "branch");
      if (!enterprise.isActiveBranch(company, branch)) {
        throw new PostingException("Posting branch is missing, inactive, or outside company scope.");
      }
    }
  }

  @Component
  public static final class CurrencyAdapter implements PostingPorts.CurrencyValidationPort {
    private final MasterDataReferencePort masterData;

    public CurrencyAdapter(final MasterDataReferencePort masterDataReferencePort) {
      masterData = masterDataReferencePort;
    }

    public void requireCurrency(final String code) {
      if (code == null || code.isBlank()) {
        throw new PostingException("Currency is required.");
      }
      if (!masterData.isActiveCurrency(code.trim().toUpperCase())) {
        throw new PostingException("Posting currency is missing or inactive.");
      }
    }
  }

  @Component
  public static final class RateAdapter implements PostingPorts.ExchangeRateValidationPort {
    private final MasterDataReferencePort masterData;
    private final EnterpriseReferencePort enterprise;

    public RateAdapter(final MasterDataReferencePort masterDataPort,
                       final EnterpriseReferencePort enterprisePort) {
      masterData = masterDataPort;
      enterprise = enterprisePort;
    }

    public BigDecimal requireRate(final UUID companyId, final String code,
                                  final BigDecimal rate, final LocalDate date) {
      if (rate == null || rate.signum() <= 0) {
        throw new IllegalArgumentException("Exchange rate is required.");
      }
      final String baseCurrency = enterprise.companyBaseCurrency(companyId)
          .orElseThrow(() -> new PostingException("Company base currency is unavailable."));
      final BigDecimal resolved;
      if (baseCurrency.equalsIgnoreCase(code)) {
        resolved = BigDecimal.ONE;
      } else {
        resolved = masterData.resolveExchangeRate(companyId, code, baseCurrency, date)
            .map(MasterDataReferencePort.ExchangeRateSnapshot::rate)
            .orElseThrow(() -> new PostingException(
                "Applicable company exchange rate is missing or expired."));
      }
      if (resolved.signum() <= 0 || resolved.compareTo(rate) != 0) {
        throw new PostingException("Submitted exchange rate does not match the authoritative rate.");
      }
      return resolved;
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
    private final EnterpriseReferencePort enterprise;
    private final MasterDataReferencePort masterData;

    public JournalAdapter(
        final FinanceService financeService,
        final FinanceRepository financeRepository,
        final PostingRuleEvaluator postingRuleEvaluator,
        final EnterpriseReferencePort enterpriseReferencePort,
        final MasterDataReferencePort masterDataReferencePort) {
      finance = financeService;
      repository = financeRepository;
      evaluator = postingRuleEvaluator;
      enterprise = enterpriseReferencePort;
      masterData = masterDataReferencePort;
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
              journalId -> snapshot(journalId, event),
              event.submittedBy());
      return new PostingPorts.JournalReference(journal.id(), journal.number());
    }

    private JournalPostingSnapshot snapshot(
        final UUID journalEntryId, final AccountingEvent event) {
      final String baseCurrency =
          enterprise
              .companyBaseCurrency(event.companyId())
              .orElseThrow(() -> new PostingException("Company base currency is unavailable."));
      final UUID rateId;
      if (baseCurrency.equalsIgnoreCase(event.currencyCode())) {
        rateId = null;
      } else {
        rateId =
            masterData
                .resolveExchangeRate(
                    event.companyId(),
                    event.currencyCode(),
                    baseCurrency,
                    event.accountingDate())
                .map(MasterDataReferencePort.ExchangeRateSnapshot::rateId)
                .orElseThrow(
                    () -> new PostingException("Authoritative exchange-rate snapshot is missing."));
      }
      final Map<String, String> taxContext =
          event.attributes().entrySet().stream()
              .filter(entry -> entry.getKey().startsWith("tax"))
              .collect(
                  java.util.stream.Collectors.toUnmodifiableMap(
                      Map.Entry::getKey, Map.Entry::getValue));
      return new JournalPostingSnapshot(
          journalEntryId,
          event.currencyCode(),
          baseCurrency,
          rateId,
          "MASTER_DATA",
          "SPOT",
          event.accountingDate(),
          event.exchangeRate(),
          event.amount(),
          event.amount().multiply(event.exchangeRate()),
          taxContext,
          java.time.Instant.now());
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
    private final PlatformAuditOutboxPort platform;

    public PlatformAdapter(final PlatformAuditOutboxPort platformPort) {
      platform = platformPort;
    }

    public void record(final String actor, final String event, final UUID id) {
      platform.recordAudit(actor, event, "FinancePosting", id, Map.of());
    }

    public void publish(final String event, final UUID id) {
      platform.publishEvent("finance-posting", event, id, Map.of());
    }
  }

  @Component
  public static final class SecurityAdapter
      implements PostingPorts.CurrentUserPort, PostingPorts.AuthorizationPort {
    private final IdentityAuthorizationPort identity;

    public SecurityAdapter(final IdentityAuthorizationPort identityAuthorizationPort) {
      identity = identityAuthorizationPort;
    }

    public String currentUser() {
      final var authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication == null || !authentication.isAuthenticated()) {
        throw new AuthenticationCredentialsNotFoundException("Authentication is required.");
      }
      validateSession(authentication);
      return authentication.getName();
    }

    public void require(final String actor, final String capability, final UUID companyId) {
      validateCurrentSession(actor);
      if (!identity.isCompanyCapabilityGranted(
          authenticatedUser(actor), capability, companyId)) {
        throw new AccessDeniedException("Permission denied for company scope.");
      }
    }

    public void requireGlobal(final String actor, final String capability) {
      validateCurrentSession(actor);
      if (!identity.isSystemEnterpriseCapabilityGranted(
          authenticatedUser(actor), capability)) {
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

    private void validateCurrentSession(final String actor) {
      final var authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication == null || !authentication.isAuthenticated()
          || !authentication.getName().equals(actor)) {
        throw new AuthenticationCredentialsNotFoundException("Authentication is required.");
      }
      validateSession(authentication);
    }

    private void validateSession(
        final org.springframework.security.core.Authentication authentication) {
      if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
        final String sessionClaim = jwtAuthentication.getToken().getClaimAsString("session_id");
        final UUID userId = authenticatedUser(authentication.getName());
        final UUID sessionId;
        try {
          sessionId = UUID.fromString(sessionClaim);
        } catch (IllegalArgumentException | NullPointerException exception) {
          throw new AuthenticationCredentialsNotFoundException(
              "Authenticated session identifier is invalid.");
        }
        if (!identity.isSessionAuthorized(userId, sessionId)) {
          throw new AuthenticationCredentialsNotFoundException(
              "Authenticated session is invalid, expired, or revoked.");
        }
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
