package com.newland.erp.crm.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newland.erp.identity.application.integration.IdentityAuthorizationPort;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

final class CrmSecurityAdapterTest {
  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void acceptsAuthoritativeSessionAndCompanyCapability() {
    final UUID userId = UUID.randomUUID();
    final UUID sessionId = UUID.randomUUID();
    final UUID companyId = UUID.randomUUID();
    authenticate(userId, sessionId);
    final var adapter =
        new CrmSecurityAdapter(identity(userId, sessionId, true, true));

    assertThat(adapter.currentActor()).isEqualTo(userId.toString());
    adapter.require(userId.toString(), "crm.activity.create", companyId);
  }

  @Test
  void rejectsRevokedSessionAndMissingCompanyCapability() {
    final UUID userId = UUID.randomUUID();
    final UUID sessionId = UUID.randomUUID();
    authenticate(userId, sessionId);
    assertThatThrownBy(
            new CrmSecurityAdapter(identity(userId, sessionId, false, true))::currentActor)
        .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
        .hasMessageContaining("revoked");

    final var unauthorized =
        new CrmSecurityAdapter(identity(userId, sessionId, true, false));
    assertThatThrownBy(
            () ->
                unauthorized.require(
                    userId.toString(), "crm.activity.create", UUID.randomUUID()))
        .isInstanceOf(AccessDeniedException.class);
  }

  private static IdentityAuthorizationPort identity(
      final UUID userId,
      final UUID sessionId,
      final boolean sessionAuthorized,
      final boolean companyAuthorized) {
    final IdentityAuthorizationPort identity = mock(IdentityAuthorizationPort.class);
    when(identity.isSessionAuthorized(userId, sessionId)).thenReturn(sessionAuthorized);
    when(identity.isCompanyCapabilityGranted(
            org.mockito.ArgumentMatchers.eq(userId),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(UUID.class)))
        .thenReturn(companyAuthorized);
    return identity;
  }

  private static void authenticate(final UUID userId, final UUID sessionId) {
    final Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "HS256")
            .subject(userId.toString())
            .claim("session_id", sessionId.toString())
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .build();
    SecurityContextHolder.getContext()
        .setAuthentication(new JwtAuthenticationToken(jwt, java.util.List.of()));
  }
}
