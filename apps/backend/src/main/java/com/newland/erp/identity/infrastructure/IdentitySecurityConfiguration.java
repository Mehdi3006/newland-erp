package com.newland.erp.identity.infrastructure;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

@Configuration
public class IdentitySecurityConfiguration {
    @Bean
    SecurityFilterChain identitySecurityFilterChain(final HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }

    @Bean
    JwtEncoder jwtEncoder(@Value("${newland.security.jwt.secret}")
                          final String secret) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(validatedSecret(secret)));
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${newland.security.jwt.secret}")
                          final String secret) {
        final SecretKeySpec key = new SecretKeySpec(validatedSecret(secret), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    @Bean
    SecureRandom secureRandom() {
        return new SecureRandom();
    }

    private static byte[] validatedSecret(final String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("newland.security.jwt.secret must be configured.");
        }
        final byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32 || secret.toLowerCase(java.util.Locale.ROOT).contains("change-me")
                || secret.toLowerCase(java.util.Locale.ROOT).contains("development-only")
                || secret.chars().distinct().count() < 12) {
            throw new IllegalStateException(
                    "newland.security.jwt.secret must contain at least 32 bytes of non-default entropy.");
        }
        return bytes;
    }
}
