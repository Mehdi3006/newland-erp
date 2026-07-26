package com.newland.erp.crm.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class CrmDomainTest {
  private static final Instant NOW = Instant.parse("2026-07-26T00:00:00Z");

  @Test
  void enforcesLeadLifecycleAndTerminalState() {
    final Lead lead = lead();
    final Lead qualified = lead.qualify(NOW.plusSeconds(1));
    final Lead converted = qualified.convert("Opportunity created", NOW.plusSeconds(2));

    assertThat(qualified.status()).isEqualTo(Lead.Status.QUALIFIED);
    assertThat(converted.status()).isEqualTo(Lead.Status.CONVERTED);
    assertThatThrownBy(() -> converted.qualify(NOW.plusSeconds(3)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void requiresLeadContactChannel() {
    assertThatThrownBy(
            () ->
                new Lead(
                    UUID.randomUUID(), "key", UUID.randomUUID(), null, UUID.randomUUID(),
                    "LEAD-1", "Organization", "Contact", "", "", "WEB", Lead.Status.NEW,
                    "", 0, NOW, NOW, UUID.randomUUID().toString()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("email address or phone");
  }

  @Test
  void enforcesSequentialOpportunityLifecycleAndTerminalImmutability() {
    Opportunity opportunity = opportunity();
    opportunity = opportunity.advance(Opportunity.Stage.DISCOVERY, "", NOW.plusSeconds(1));
    opportunity = opportunity.advance(Opportunity.Stage.PROPOSAL, "", NOW.plusSeconds(2));
    opportunity = opportunity.advance(Opportunity.Stage.NEGOTIATION, "", NOW.plusSeconds(3));
    final Opportunity won =
        opportunity.advance(Opportunity.Stage.WON, "Contract accepted", NOW.plusSeconds(4));

    assertThat(won.probabilityPercent()).isEqualTo(100);
    assertThatThrownBy(
            () -> won.advance(Opportunity.Stage.LOST, "changed", NOW.plusSeconds(5)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("immutable");
  }

  @Test
  void rejectsOpportunityWithoutLeadOrCustomerAndInvalidActivity() {
    assertThatThrownBy(
            () ->
                new Opportunity(
                    UUID.randomUUID(), "key", UUID.randomUUID(), null, UUID.randomUUID(),
                    null, null, "OPP-1", "Opportunity", Opportunity.Stage.QUALIFICATION,
                    BigDecimal.TEN, "USD", 20, LocalDate.now().plusDays(10), "", 0,
                    NOW, NOW, UUID.randomUUID().toString()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("lead or customer");

    assertThatThrownBy(
            () ->
                new Activity(
                    UUID.randomUUID(), "activity", UUID.randomUUID(), null, null, null,
                    Activity.Type.CALL, "Call", "", NOW, null, UUID.randomUUID().toString()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("reference");
  }

  static Lead lead() {
    return new Lead(
        UUID.randomUUID(), "lead-key", UUID.randomUUID(), null, UUID.randomUUID(),
        "LEAD-1", "Organization", "Contact", "contact@example.com", "", "WEB",
        Lead.Status.NEW, "", 0, NOW, NOW, UUID.randomUUID().toString());
  }

  static Opportunity opportunity() {
    return new Opportunity(
        UUID.randomUUID(), "opportunity-key", UUID.randomUUID(), null, UUID.randomUUID(),
        UUID.randomUUID(), null, "OPP-1", "Opportunity", Opportunity.Stage.QUALIFICATION,
        new BigDecimal("1000"), "USD", 20, LocalDate.of(2026, 9, 1), "", 0,
        NOW, NOW, UUID.randomUUID().toString());
  }
}
