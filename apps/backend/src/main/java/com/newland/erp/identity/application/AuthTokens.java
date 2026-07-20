package com.newland.erp.identity.application;

import java.time.Instant;
import java.util.UUID;

public record AuthTokens(String accessToken, String refreshToken, Instant accessTokenExpiresAt,
                         Instant refreshTokenExpiresAt, UUID sessionId) {
}
