package com.newland.erp.logistics.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public final class ShipmentTest {
  @Test
  void requiresBookingBeforeContainerLoadingAndTracksCustomsRelease() {
    final Shipment draft = shipment();
    final Shipment.Container container =
        new Shipment.Container(
            UUID.randomUUID(), "mscu-100", "40hc", new BigDecimal("1000"),
            new BigDecimal("60"), null);

    assertThatThrownBy(() -> draft.addContainer(container))
        .isInstanceOf(IllegalStateException.class);

    final Shipment booked = draft.book().addContainer(container);
    final Shipment loaded = booked.loadContainer(container.id(), Instant.now());
    final Shipment released =
        loaded.recordMilestone(
            new Shipment.CustomsMilestone(
                UUID.randomUUID(), Shipment.MilestoneType.CUSTOMS_RELEASED,
                "release-1", Instant.now(), "cleared"));

    assertThat(loaded.status()).isEqualTo(Shipment.Status.IN_TRANSIT);
    assertThat(loaded.containers().getFirst().loadedAt()).isNotNull();
    assertThat(released.status()).isEqualTo(Shipment.Status.CUSTOMS_RELEASED);
  }

  @Test
  void calculatesLandedCostDraftWithoutPostingIt() {
    final LandedCostDraft draft =
        new LandedCostDraft(
            UUID.randomUUID(), UUID.randomUUID(), "cost-1", "USD",
            LandedCostDraft.AllocationBasis.WEIGHT,
            List.of(
                new LandedCostDraft.CostComponent(
                    UUID.randomUUID(), "FREIGHT", new BigDecimal("120.50"), "BL-1"),
                new LandedCostDraft.CostComponent(
                    UUID.randomUUID(), "CUSTOMS", new BigDecimal("30.25"), "CUS-1")),
            Instant.now(), "actor");

    assertThat(draft.total()).isEqualByComparingTo("150.75");
  }

  public static Shipment shipment() {
    return new Shipment(
        UUID.randomUUID(), "shp-1", "key-1", UUID.randomUUID(), UUID.randomUUID(),
        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "carrier", "cnszx", "irbnd",
        "FOB", LocalDate.now(), LocalDate.now().plusDays(20), Shipment.Status.DRAFT,
        List.of(), List.of(), 0, Instant.now(), "actor");
  }
}
