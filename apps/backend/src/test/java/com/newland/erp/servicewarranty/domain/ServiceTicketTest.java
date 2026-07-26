package com.newland.erp.servicewarranty.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class ServiceTicketTest {
  private static final Instant NOW = Instant.parse("2026-07-26T00:00:00Z");

  @Test
  void followsWarrantyDiagnosisResolutionAndClosureLifecycle() {
    final ServiceTicket validating = ticket().beginValidation(NOW.plusSeconds(1));
    final ServiceTicket valid =
        validating.recordWarranty(
            new ServiceTicket.WarrantyDecision(
                UUID.randomUUID(), UUID.randomUUID(), true, "Covered",
                LocalDate.of(2027, 1, 1), NOW.plusSeconds(2), "actor"),
            NOW.plusSeconds(2));
    final ServiceTicket diagnosed =
        valid.diagnose(
            new ServiceTicket.Diagnosis(
                UUID.randomUUID(), "Power failure", "Replace board", NOW.plusSeconds(3)),
            NOW.plusSeconds(3));
    final ServiceTicket repairing =
        diagnosed.approveResolution(
            ServiceTicket.Resolution.Type.REPAIR, "Repair approved", NOW.plusSeconds(4));
    final ServiceTicket closed = repairing.close("Repair completed", NOW.plusSeconds(5));

    assertThat(closed.status()).isEqualTo(ServiceTicket.Status.CLOSED);
    assertThat(closed.resolution().outcome()).isEqualTo("Repair completed");
    assertThatThrownBy(() -> closed.close("again", NOW.plusSeconds(6)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void rejectsOutOfOrderTransitionsAndClosureWithoutResolution() {
    final ServiceTicket ticket = ticket();
    assertThatThrownBy(
            () ->
                ticket.diagnose(
                    new ServiceTicket.Diagnosis(
                        UUID.randomUUID(), "Finding", "Repair", NOW),
                    NOW))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> ticket.close("Closed", NOW))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void validatesWarrantyPolicyConfigurationAndApplicability() {
    final UUID productId = UUID.randomUUID();
    final WarrantyPolicy policy =
        new WarrantyPolicy(
            UUID.randomUUID(), UUID.randomUUID(), productId, 365, true, true,
            LocalDate.of(2026, 1, 1), null, true);

    assertThat(policy.applies(productId, LocalDate.of(2026, 7, 1))).isTrue();
    assertThat(policy.applies(UUID.randomUUID(), LocalDate.of(2026, 7, 1))).isFalse();
  }

  static ServiceTicket ticket() {
    return new ServiceTicket(
        UUID.randomUUID(), "ticket-key", "SRV-1", UUID.randomUUID(), UUID.randomUUID(),
        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "SERIAL-1",
        UUID.randomUUID(), LocalDate.of(2026, 1, 1), "Device will not start",
        ServiceTicket.Status.OPEN, null, null, null, 0, NOW, NOW, "actor");
  }
}
