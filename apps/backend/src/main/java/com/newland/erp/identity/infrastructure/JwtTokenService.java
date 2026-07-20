package com.newland.erp.identity.infrastructure;

import com.newland.erp.identity.application.TokenService;
import com.newland.erp.identity.domain.User;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

@Component
public final class JwtTokenService implements TokenService {
    private final JwtEncoder jwtEncoder;
    private final SecureRandom secureRandom;

    public JwtTokenService(final JwtEncoder encoder, final SecureRandom random) {
        this.jwtEncoder = encoder;
        this.secureRandom = random;
    }

    @Override
    public String issueAccessToken(final User user, final Set<String> capabilities, final UUID sessionId,
                                   final Instant expiresAt) {
        final Instant now = Instant.now();
        final JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("newland-erp")
                .subject(user.id().toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("username", user.username().value())
                .claim("session_id", sessionId.toString())
                .claim("capabilities", capabilities.stream().sorted().toList())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    @Override
    public String newRefreshToken() {
        final byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public String tokenHash(final String token) {
        try {
            final byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is required for refresh token hashing.", exception);
        }
    }
}
