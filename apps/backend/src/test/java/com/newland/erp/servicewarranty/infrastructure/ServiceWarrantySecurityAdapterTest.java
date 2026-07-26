package com.newland.erp.servicewarranty.infrastructure;

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

final class ServiceWarrantySecurityAdapterTest {
  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void requiresAuthoritativeSessionAndCompanyCapability() {
    final UUID userId = UUID.randomUUID();
    final UUID sessionId = UUID.randomUUID();
    authenticate(userId, sessionId);
    final var authorized =
        new ServiceWarrantySecurityAdapter(identity(userId, sessionId, true, true));
    assertThat(authorized.currentActor()).isEqualTo(userId.toString());
    authorized.require(userId.toString(), "service.ticket.manage", UUID.randomUUID());

    final var revoked =
        new ServiceWarrantySecurityAdapter(identity(userId, sessionId, false, true));
    assertThatThrownBy(revoked::currentActor)
        .isInstanceOf(AuthenticationCredentialsNotFoundException.class);

    final var denied =
        new ServiceWarrantySecurityAdapter(identity(userId, sessionId, true, false));
    assertThatThrownBy(
            () -> denied.require(userId.toString(), "service.ticket.manage", UUID.randomUUID()))
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
