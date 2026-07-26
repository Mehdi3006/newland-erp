package com.newland.erp.finance.infrastructure;

import com.newland.erp.finance.application.FinancePorts;
import com.newland.erp.finance.domain.FinanceException;
import com.newland.erp.finance.domain.JournalEntry;
import com.newland.erp.finance.domain.JournalPostingSnapshot;
import com.newland.erp.enterprise.application.integration.EnterpriseReferencePort;
import com.newland.erp.identity.application.integration.IdentityAuthorizationPort;
import com.newland.erp.masterdata.application.integration.MasterDataReferencePort;
import com.newland.erp.platform.application.integration.PlatformAuditOutboxPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

public final class FinanceInfrastructureAdapters {
  @Component
  public static final class EnterpriseAdapter implements FinancePorts.EnterprisePort {
    private final EnterpriseReferencePort enterprise;

    public EnterpriseAdapter(final EnterpriseReferencePort enterpriseReferencePort) {
      enterprise = enterpriseReferencePort;
    }

    public void requireCompanyBranch(final UUID company, final UUID branch) {
      if (company == null || branch == null) {
        throw new IllegalArgumentException("Company and branch scope are required.");
      }
      if (!enterprise.isActiveCompany(company) || !enterprise.isActiveBranch(company, branch)) {
        throw new IllegalArgumentException(
            "Company or branch is inactive or outside the requested scope.");
      }
    }
  }

  @Component
  public static final class PostingSnapshotAdapter
      implements FinancePorts.PostingSnapshotPort {
    private final EnterpriseReferencePort enterprise;
    private final MasterDataReferencePort masterData;
    private final DSLContext dsl;

    public PostingSnapshotAdapter(
        final EnterpriseReferencePort enterpriseReferencePort,
        final MasterDataReferencePort masterDataReferencePort,
        final DSLContext dslContext) {
      enterprise = enterpriseReferencePort;
      masterData = masterDataReferencePort;
      dsl = dslContext;
    }

    @Override
    public JournalPostingSnapshot resolve(
        final JournalEntry journal,
        final Map<String, String> taxContext,
        final Instant postedAt) {
      final String baseCurrency =
          enterprise
              .companyBaseCurrency(journal.companyId())
              .orElseThrow(() -> new FinanceException("Company accounting currency is unavailable."));
      final var currencyIds =
          journal.lines().stream()
              .map(JournalEntry.JournalLine::currencyId)
              .filter(Objects::nonNull)
              .distinct()
              .toList();
      if (currencyIds.size() > 1) {
        throw new FinanceException("A manual journal must use one transaction currency.");
      }
      final String transactionCurrency =
          currencyIds.isEmpty() ? baseCurrency : currencyCode(currencyIds.getFirst());
      final BigDecimal amount =
          journal.lines().stream()
              .map(JournalEntry.JournalLine::debit)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      if (transactionCurrency.equals(baseCurrency)) {
        return new JournalPostingSnapshot(
            journal.id(),
            transactionCurrency,
            baseCurrency,
            null,
            "ENTERPRISE",
            "SPOT",
            journal.postingDate(),
            BigDecimal.ONE,
            amount,
            amount,
            taxContext,
            postedAt);
      }
      final MasterDataReferencePort.ExchangeRateSnapshot rate =
          masterData
              .resolveExchangeRate(
                  journal.companyId(),
                  transactionCurrency,
                  baseCurrency,
                  journal.postingDate())
              .orElseThrow(
                  () -> new FinanceException("Authoritative exchange-rate snapshot is missing."));
      journal.lines().stream()
          .map(JournalEntry.JournalLine::exchangeRateSnapshot)
          .filter(Objects::nonNull)
          .filter(value -> value.compareTo(rate.rate()) != 0)
          .findAny()
          .ifPresent(
              ignored -> {
                throw new FinanceException(
                    "Journal line exchange rate differs from the authoritative rate.");
              });
      return new JournalPostingSnapshot(
          journal.id(),
          transactionCurrency,
          baseCurrency,
          rate.rateId(),
          "MASTER_DATA",
          "SPOT",
          journal.postingDate(),
          rate.rate(),
          amount,
          amount.multiply(rate.rate()),
          taxContext,
          postedAt);
    }

    private String currencyCode(final UUID currencyId) {
      final String code =
          dsl.select(DSL.field("code", String.class))
              .from(DSL.table("master_data_record"))
              .where(DSL.field("id", UUID.class).eq(currencyId))
              .and(DSL.field("aggregate_type", String.class).eq("CURRENCY"))
              .and(DSL.field("active", Boolean.class).eq(true))
              .fetchOne(0, String.class);
      if (code == null) {
        throw new FinanceException("Transaction currency is missing or inactive.");
      }
      return code;
    }
  }

  @Component
  public static final class MasterDataAdapter implements FinancePorts.MasterDataPort {
    private final DSLContext dsl;

    public MasterDataAdapter(final DSLContext dslContext) {
      dsl = dslContext;
    }

    public void requireCurrency(final UUID currency) {
      if (currency == null) {
        throw new IllegalArgumentException("Currency is required.");
      }
      final boolean active =
          dsl.fetchExists(
              DSL.table("master_data_record"),
              DSL.field("id", UUID.class)
                  .eq(currency)
                  .and(DSL.field("aggregate_type", String.class).eq("CURRENCY"))
                  .and(DSL.field("active", Boolean.class).eq(true)));
      if (!active) {
        throw new IllegalArgumentException("Currency is missing or inactive.");
      }
    }
  }

  @Component
  public static final class AuthorizationAdapter implements FinancePorts.AuthorizationPort {
    private final IdentityAuthorizationPort identity;

    public AuthorizationAdapter(final IdentityAuthorizationPort identityAuthorizationPort) {
      identity = identityAuthorizationPort;
    }

    public void require(final String actor, final String capability, final UUID company) {
      final UUID userId = requireCurrentSession(actor);
      if (company == null
          || !identity.isCompanyCapabilityGranted(userId, capability, company)) {
        throw new AccessDeniedException("Finance permission denied for company scope.");
      }
    }

    public void requireCostCenter(final String actor, final UUID id) {
      if (id == null) {
        throw new IllegalArgumentException("Cost center is required.");
      }
      requireCurrentSession(actor);
    }

    public void requireProfitCenter(final String actor, final UUID id) {
      if (id == null) {
        throw new IllegalArgumentException("Profit center is required.");
      }
      requireCurrentSession(actor);
    }

    public void requireDimension(final String actor, final String id) {
      if (id == null || id.isBlank()) {
        throw new IllegalArgumentException("Dimension is required.");
      }
      requireCurrentSession(actor);
    }

    private UUID requireCurrentSession(final String actor) {
      final var authentication = SecurityContextHolder.getContext().getAuthentication();
      if (!(authentication instanceof JwtAuthenticationToken token)
          || !authentication.isAuthenticated()
          || actor == null
          || !authentication.getName().equals(actor)) {
        throw new AuthenticationCredentialsNotFoundException("Authentication is required.");
      }
      final UUID userId = identifier(authentication.getName());
      final UUID sessionId = identifier(token.getToken().getClaimAsString("session_id"));
      if (!identity.isSessionAuthorized(userId, sessionId)) {
        throw new AuthenticationCredentialsNotFoundException(
            "Authenticated session is invalid, expired, or revoked.");
      }
      return userId;
    }

    private static UUID identifier(final String value) {
      try {
        return UUID.fromString(value);
      } catch (IllegalArgumentException | NullPointerException exception) {
        throw new AuthenticationCredentialsNotFoundException("Invalid identity context.");
      }
    }
  }

  @Component
  public static final class NumberAdapter implements FinancePorts.NumberSeriesPort {
    private final DSLContext dsl;

    public NumberAdapter(final DSLContext dslContext) {
      dsl = dslContext;
    }

    public String next(final String s) {
      final Long value =
          dsl.select(DSL.field("nextval('finance_journal_number_seq')", Long.class))
              .fetchOne(0, Long.class);
      return s + "-" + value;
    }
  }

  @Component
  public static final class PlatformAdapter
      implements FinancePorts.AuditPort, FinancePorts.OutboxPort, FinancePorts.AttachmentPort {
    private final PlatformAuditOutboxPort platform;

    public PlatformAdapter(final PlatformAuditOutboxPort platformPort) {
      platform = platformPort;
    }

    public void record(final String actor, final String action, final UUID id) {
      platform.recordAudit(actor, action, "FinanceJournal", id, Map.of());
    }

    public void publish(final String eventType, final UUID id) {
      platform.publishEvent("finance", eventType, id, Map.of());
    }

    public void attach(final UUID aggregateId, final UUID attachmentId) {
      if (attachmentId == null) {
        throw new IllegalArgumentException("Attachment is required.");
      }
      platform.attachFile("finance", "JournalEntry", aggregateId, attachmentId);
    }
  }

  private FinanceInfrastructureAdapters() {}
}
