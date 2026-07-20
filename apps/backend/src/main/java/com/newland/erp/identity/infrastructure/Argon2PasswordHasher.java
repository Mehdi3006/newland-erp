package com.newland.erp.identity.infrastructure;

import com.newland.erp.identity.application.PasswordHasher;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public final class Argon2PasswordHasher implements PasswordHasher {
    private final Argon2PasswordEncoder encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

    @Override
    public String hash(final String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(final String rawPassword, final String hash) {
        return encoder.matches(rawPassword, hash);
    }
}
