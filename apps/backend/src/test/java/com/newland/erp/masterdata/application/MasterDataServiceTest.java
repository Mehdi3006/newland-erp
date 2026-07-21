package com.newland.erp.masterdata.application;

import com.newland.erp.masterdata.domain.DuplicateMasterDataCodeException;
import com.newland.erp.masterdata.domain.MasterDataType;
import com.newland.erp.masterdata.domain.MasterDataVersionConflictException;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class MasterDataServiceTest {
    private final InMemoryMasterDataRepository repository = new InMemoryMasterDataRepository();
    private final MasterDataService service = new MasterDataService(repository,
            Clock.fixed(Instant.parse("2026-07-21T00:00:00Z"), ZoneOffset.UTC));

    @Test
    void createsAndListsConfiguredAggregateType() {
        final var created = service.create(new MasterDataCommands.Create(MasterDataType.PAYMENT_TERMS,
                "NET30", "Net 30", null, Map.of("days", "30")));

        assertThat(created.active()).isTrue();
        assertThat(service.list(MasterDataType.PAYMENT_TERMS)).containsExactly(created);
    }

    @Test
    void rejectsDuplicateCodesWithinAggregateType() {
        service.create(new MasterDataCommands.Create(MasterDataType.CURRENCY, "USD", "US Dollar", null,
                Map.of()));

        assertThatThrownBy(() -> service.create(new MasterDataCommands.Create(MasterDataType.CURRENCY,
                "usd", "Duplicate Dollar", null, Map.of())))
                .isInstanceOf(DuplicateMasterDataCodeException.class);
    }

    @Test
    void protectsUpdatesWithExpectedVersion() {
        final var created = service.create(new MasterDataCommands.Create(MasterDataType.BARCODE_TYPE,
                "EAN13", "EAN-13", null, Map.of()));

        assertThatThrownBy(() -> service.update(new MasterDataCommands.Update(created.id(), "EAN", Map.of(), 99)))
                .isInstanceOf(MasterDataVersionConflictException.class);
    }
}
