package com.newland.erp.finance.posting.infrastructure;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newland.erp.identity.application.integration.IdentityAuthorizationPort;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

final class PostingSecurityAdapterTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsRevokedSessionBeforeCapabilityEvaluation() {
        final UUID userId = UUID.randomUUID();
        final UUID sessionId = UUID.randomUUID();
        final IdentityAuthorizationPort identity = mock(IdentityAuthorizationPort.class);
        when(identity.isSessionAuthorized(userId, sessionId)).thenReturn(false);
        final Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject(userId.toString())
                .claim("session_id", sessionId.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, java.util.List.of()));
        final var adapter = new PostingInfrastructureAdapters.SecurityAdapter(identity);

        assertThatThrownBy(() ->
                adapter.require(userId.toString(), "finance.posting.submit", UUID.randomUUID()))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    void rejectsMissingOrInvalidSessionClaim() {
        final UUID userId = UUID.randomUUID();
        final Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject(userId.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, java.util.List.of()));

        assertThatThrownBy(
                () -> new PostingInfrastructureAdapters.SecurityAdapter(
                        mock(IdentityAuthorizationPort.class)).currentUser())
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
                .hasMessageContaining("session identifier");
    }
}
