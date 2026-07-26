package com.newland.erp.finance.posting.application.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class FinancePostingContractValidationTest {
  @Test
  void normalizesCurrencyAndDefensivelyCopiesContractMaps() {
    final Map<String, String> dimensions = new HashMap<>();
    dimensions.put("costCenter", "CC-1");
    final var message = message("usd", BigDecimal.ONE, dimensions);
    dimensions.clear();

    assertThat(message.currencyCode()).isEqualTo("USD");
    assertThat(message.dimensions()).containsEntry("costCenter", "CC-1");
  }

  @Test
  void rejectsInvalidExchangeRateAndCurrency() {
    assertThatThrownBy(() -> message("US", BigDecimal.ONE, Map.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Currency");
    assertThatThrownBy(() -> message("USD", BigDecimal.ZERO, Map.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Exchange rate");
  }

  private static FinancePostingIntegrationPort.AccountingEventMessage message(
      final String currency,
      final BigDecimal rate,
      final Map<String, String> dimensions) {
    return new FinancePostingIntegrationPort.AccountingEventMessage(
        UUID.randomUUID(),
        "event-key",
        "SourceDocumentPosted",
        "source",
        "SourceDocument",
        UUID.randomUUID(),
        "SRC-1",
        UUID.randomUUID(),
        UUID.randomUUID(),
        LocalDate.of(2026, 1, 31),
        LocalDate.of(2026, 1, 31),
        currency,
        rate,
        BigDecimal.TEN,
        BigDecimal.ONE,
        new BigDecimal("9"),
        "Posting contract",
        dimensions,
        Map.of(),
        Instant.parse("2026-01-31T00:00:00Z"),
        UUID.randomUUID().toString());
  }
}
