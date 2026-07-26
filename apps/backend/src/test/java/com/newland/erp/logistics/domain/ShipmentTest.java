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
    final Instant departedAt = Instant.now();
    final Shipment departed =
        milestone(loaded, Shipment.MilestoneType.DEPARTED, "departed-1", departedAt);
    final Shipment arrived =
        milestone(departed, Shipment.MilestoneType.ARRIVED_PORT, "arrival-1",
            departedAt.plusSeconds(1));
    final Shipment filed =
        milestone(arrived, Shipment.MilestoneType.CUSTOMS_FILED, "filing-1",
            departedAt.plusSeconds(2));
    final Shipment released =
        milestone(filed, Shipment.MilestoneType.CUSTOMS_RELEASED, "release-1",
            departedAt.plusSeconds(3));

    assertThat(loaded.status()).isEqualTo(Shipment.Status.IN_TRANSIT);
    assertThat(loaded.containers().getFirst().loadedAt()).isNotNull();
    assertThat(released.status()).isEqualTo(Shipment.Status.CUSTOMS_RELEASED);
  }

  @Test
  void rejectsInvalidBackwardAndOutOfOrderMilestones() {
    final Shipment loaded = loadedShipment();
    final Instant now = Instant.now();

    assertThatThrownBy(
            () -> milestone(loaded, Shipment.MilestoneType.CUSTOMS_RELEASED, "release", now))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Invalid customs milestone transition");

    final Shipment departed =
        milestone(loaded, Shipment.MilestoneType.DEPARTED, "departed", now);
    final Shipment arrived =
        milestone(departed, Shipment.MilestoneType.ARRIVED_PORT, "arrived",
            now.plusSeconds(2));

    assertThatThrownBy(
            () -> milestone(arrived, Shipment.MilestoneType.DEPARTED, "departed-again",
                now.plusSeconds(3)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Invalid customs milestone transition");
    assertThatThrownBy(
            () -> milestone(arrived, Shipment.MilestoneType.CUSTOMS_FILED, "filed",
                now.minusSeconds(1)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("time cannot move backwards");
  }

  @Test
  void terminalShipmentRejectsFurtherMilestones() {
    final Shipment loaded = loadedShipment();
    final Instant now = Instant.now();
    final Shipment departed =
        milestone(loaded, Shipment.MilestoneType.DEPARTED, "departed-terminal", now);
    final Shipment arrived =
        milestone(departed, Shipment.MilestoneType.ARRIVED_PORT, "arrived-terminal",
            now.plusSeconds(1));
    final Shipment filed =
        milestone(arrived, Shipment.MilestoneType.CUSTOMS_FILED, "filed-terminal",
            now.plusSeconds(2));
    final Shipment released =
        milestone(filed, Shipment.MilestoneType.CUSTOMS_RELEASED, "released-terminal",
            now.plusSeconds(3));
    final Shipment delivered =
        milestone(released, Shipment.MilestoneType.INLAND_DELIVERY, "delivered-terminal",
            now.plusSeconds(4));

    assertThat(delivered.status()).isEqualTo(Shipment.Status.DELIVERED);
    assertThatThrownBy(
            () -> milestone(delivered, Shipment.MilestoneType.CUSTOMS_HOLD, "late-hold",
                now.plusSeconds(5)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Terminal shipments");
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

  private static Shipment loadedShipment() {
    final Shipment.Container container =
        new Shipment.Container(
            UUID.randomUUID(), UUID.randomUUID().toString(), "40HC",
            new BigDecimal("1000"), new BigDecimal("60"), null);
    return shipment().book().addContainer(container).loadContainer(container.id(), Instant.now());
  }

  private static Shipment milestone(
      final Shipment shipment,
      final Shipment.MilestoneType type,
      final String reference,
      final Instant occurredAt) {
    return shipment.recordMilestone(
        new Shipment.CustomsMilestone(
            UUID.randomUUID(), type, reference, occurredAt, "test milestone"));
  }
}
