package com.newland.erp.enterprise.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class EnterpriseStructureDomainTest {
    @Test
    void normalizesBusinessCodesAndLocalizedNames() {
        final EnterpriseCode code = new EnterpriseCode(" nl-main ");
        final LocalizedName localizedName = new LocalizedName(Map.of("EN-US", "Newland", "fa-IR", "نیولند"));

        assertThat(code.value()).isEqualTo("NL-MAIN");
        assertThat(localizedName.values()).containsEntry("en-us", "Newland").containsEntry("fa-ir", "نیولند");
    }

    @Test
    void rejectsInvalidCountryCurrencyAndTimeZoneValues() {
        assertThatThrownBy(() -> new CountryCode("USA")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CurrencyCode("IR")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TimeZoneId("Mars/Phobos")).isInstanceOf(RuntimeException.class);
    }

    @Test
    void enforcesLifecycleTransitions() {
        assertThat(LifecycleStatus.DRAFT.activate()).isEqualTo(LifecycleStatus.ACTIVE);
        assertThat(LifecycleStatus.ACTIVE.deactivate()).isEqualTo(LifecycleStatus.INACTIVE);
        assertThatThrownBy(LifecycleStatus.DRAFT::deactivate)
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void enforcesWarehouseParentRules() {
        final AuditMetadata audit = AuditMetadata.created(Instant.parse("2026-07-20T00:00:00Z"), "tester");

        assertThatThrownBy(() -> new Warehouse(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
                new WarehouseCode("BR-1"), new DisplayName("Branch warehouse"), new LocalizedName(Map.of()),
                WarehouseType.BRANCH, null, null, LifecycleStatus.DRAFT, audit))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new Warehouse(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
                new WarehouseCode("PR-1"), new DisplayName("Project warehouse"), new LocalizedName(Map.of()),
                WarehouseType.PROJECT, " ", null, LifecycleStatus.DRAFT, audit))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
