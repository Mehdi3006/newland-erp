package com.newland.erp.platform.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class PlatformDomainTest {
    @Test
    void createsPendingOutboxMessageForDomainEvent() {
        final PlatformDomainEvent event = new PlatformDomainEvent(UUID.randomUUID(), "identity",
                "UserCreated", UUID.randomUUID(), Instant.parse("2026-07-20T00:00:00Z"),
                Map.of("user", "owner"));

        final OutboxMessage message = OutboxMessage.pending(event, Instant.parse("2026-07-20T00:00:01Z"));

        assertThat(message.status()).isEqualTo(OutboxStatus.PENDING);
        assertThat(message.payload()).containsEntry("user", "owner");
    }

    @Test
    void rejectsInvalidPlatformRecords() {
        assertThatThrownBy(() -> new StoredFile(UUID.randomUUID(), "key", "file.txt", "text/plain", -1,
                "a".repeat(64), Instant.parse("2026-07-20T00:00:00Z")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
