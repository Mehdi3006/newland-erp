package com.newland.erp.identity.application;

import com.newland.erp.identity.domain.User;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public interface TokenService {
    String issueAccessToken(User user, Set<String> capabilities, UUID sessionId, Instant expiresAt);

    String newRefreshToken();

    String tokenHash(String token);
}
