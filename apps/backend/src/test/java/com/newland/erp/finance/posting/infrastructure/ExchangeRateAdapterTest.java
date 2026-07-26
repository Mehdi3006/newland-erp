package com.newland.erp.finance.posting.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.newland.erp.enterprise.application.integration.EnterpriseReferencePort;
import com.newland.erp.finance.posting.domain.PostingException;
import com.newland.erp.masterdata.application.integration.MasterDataReferencePort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class ExchangeRateAdapterTest {
    private static final LocalDate DATE = LocalDate.parse("2026-07-26");

    @Test
    void acceptsAuthoritativeCompanyRateAndRejectsMismatch() {
        final UUID companyId = UUID.randomUUID();
        final BigDecimal authoritative = new BigDecimal("1.12500000");
        final MasterDataReferencePort masterData = rates(companyId, authoritative);
        final EnterpriseReferencePort enterprise = enterprise(companyId, "USD");
        final var adapter = new PostingInfrastructureAdapters.RateAdapter(masterData, enterprise);

        assertThat(adapter.requireRate(companyId, "EUR", authoritative, DATE))
                .isEqualByComparingTo(authoritative);
        assertThatThrownBy(() ->
                adapter.requireRate(companyId, "EUR", new BigDecimal("1.20"), DATE))
                .isInstanceOf(PostingException.class)
                .hasMessageContaining("authoritative");
        assertThatThrownBy(() ->
                adapter.requireRate(UUID.randomUUID(), "EUR", authoritative, DATE))
                .isInstanceOf(PostingException.class);
    }

    @Test
    void baseCurrencyRequiresUnitRate() {
        final UUID companyId = UUID.randomUUID();
        final var adapter = new PostingInfrastructureAdapters.RateAdapter(
                rates(companyId, BigDecimal.TEN), enterprise(companyId, "USD"));

        assertThat(adapter.requireRate(companyId, "USD", BigDecimal.ONE, DATE))
                .isEqualByComparingTo(BigDecimal.ONE);
        assertThatThrownBy(() ->
                adapter.requireRate(companyId, "USD", new BigDecimal("1.01"), DATE))
                .isInstanceOf(PostingException.class);
    }

    private static MasterDataReferencePort rates(
            final UUID expectedCompany, final BigDecimal rate) {
        return new MasterDataReferencePort() {
            @Override
            public boolean isActiveCurrency(final String currencyCode) {
                return true;
            }

            @Override
            public Optional<ExchangeRateSnapshot> resolveExchangeRate(
                    final UUID companyId, final String sourceCurrency,
                    final String targetCurrency, final LocalDate effectiveDate) {
                if (!companyId.equals(expectedCompany) || !"EUR".equals(sourceCurrency)
                        || !"USD".equals(targetCurrency) || !DATE.equals(effectiveDate)) {
                    return Optional.empty();
                }
                return Optional.of(new ExchangeRateSnapshot(
                        UUID.randomUUID(), companyId, sourceCurrency, targetCurrency,
                        DATE.minusDays(1), DATE.plusDays(1), rate));
            }
        };
    }

    private static EnterpriseReferencePort enterprise(
            final UUID expectedCompany, final String baseCurrency) {
        return new EnterpriseReferencePort() {
            @Override
            public boolean isActiveCompany(final UUID companyId) {
                return companyId.equals(expectedCompany);
            }

            @Override
            public boolean isActiveBranch(final UUID companyId, final UUID branchId) {
                return companyId.equals(expectedCompany);
            }

            @Override
            public Optional<String> companyBaseCurrency(final UUID companyId) {
                return companyId.equals(expectedCompany)
                        ? Optional.of(baseCurrency) : Optional.empty();
            }
        };
    }
}
