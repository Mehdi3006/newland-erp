package com.newland.erp.identity.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class IdentityDomainTest {
    @Test
    void normalizesUsernamesAndRoleCodes() {
        assertThat(new Username(" OWNER ").value()).isEqualTo("owner");
        assertThat(new Role(UUID.randomUUID(), "security-admin", "Security Admin", null, false).code())
                .isEqualTo("SECURITY-ADMIN");
    }

    @Test
    void enforcesCapabilityAndScopeShape() {
        assertThat(new Capability("identity.user.manage").value()).isEqualTo("identity.user.manage");
        assertThatThrownBy(() -> new Capability("ADMIN")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OrganizationScope(null, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void neverAcceptsPlaintextPasswordCredential() {
        assertThatThrownBy(() -> new PasswordCredential(UUID.randomUUID(), UUID.randomUUID(), "plaintext",
                Instant.parse("2026-07-20T00:00:00Z"), null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void locksUserAfterFailedAttempts() {
        final Instant now = Instant.parse("2026-07-20T00:00:00Z");
        final User user = new User(UUID.randomUUID(), new Username("owner"),
                new EmailAddress("owner@example.com"), "Owner", UserStatus.ACTIVE, 4, null,
                now.plusSeconds(3600), now, now);

        final User locked = user.recordFailedLogin(5, java.time.Duration.ofMinutes(15), now);

        assertThat(locked.status()).isEqualTo(UserStatus.LOCKED);
        assertThat(locked.canAuthenticate(now)).isFalse();
    }
}
