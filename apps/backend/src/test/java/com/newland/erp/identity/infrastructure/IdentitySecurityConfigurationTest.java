package com.newland.erp.identity.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

final class IdentitySecurityConfigurationTest {
    private final IdentitySecurityConfiguration configuration = new IdentitySecurityConfiguration();

    @Test
    void rejectsAbsentBlankAndPredictableSecrets() {
        assertThatThrownBy(() -> configuration.jwtEncoder(null))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> configuration.jwtDecoder("  "))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> configuration.jwtEncoder("development-only-change-me-32-bytes-minimum"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> configuration.jwtEncoder("a".repeat(64)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void acceptsExplicitHighEntropySecret() {
        final String secret = "A9!" + "rotating-production-key-material-".repeat(2) + "Z7$";

        assertThat(configuration.jwtEncoder(secret)).isNotNull();
        assertThat(configuration.jwtDecoder(secret)).isNotNull();
    }
}
