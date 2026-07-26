package com.newland.erp.procurement.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

final class ProcurementAccountingSecurityAdapterTest {
  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void acceptsAnAuthoritativeSessionAndCompanyCapability() {
    final UUID userId = UUID.randomUUID();
    final UUID sessionId = UUID.randomUUID();
    final UUID companyId = UUID.randomUUID();
    final IdentityAuthorizationPort identity = identity(userId, sessionId, true, true);
    authenticate(userId, sessionId);
    final var adapter = new ProcurementAccountingSecurityAdapter(identity);

    assertThat(adapter.currentActor()).isEqualTo(userId.toString());
    adapter.requireCompanyCapability(userId.toString(), "procurement.finance.post", companyId);

    verify(identity)
        .isCompanyCapabilityGranted(userId, "procurement.finance.post", companyId);
  }

  @Test
  void rejectsRevokedSessionBeforeCompanyAuthorization() {
    final UUID userId = UUID.randomUUID();
    final UUID sessionId = UUID.randomUUID();
    authenticate(userId, sessionId);
    final var adapter =
        new ProcurementAccountingSecurityAdapter(identity(userId, sessionId, false, true));

    assertThatThrownBy(adapter::currentActor)
        .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
        .hasMessageContaining("revoked");
  }

  @Test
  void rejectsMissingCompanyCapability() {
    final UUID userId = UUID.randomUUID();
    final UUID sessionId = UUID.randomUUID();
    authenticate(userId, sessionId);
    final var adapter =
        new ProcurementAccountingSecurityAdapter(identity(userId, sessionId, true, false));

    assertThatThrownBy(
            () ->
                adapter.requireCompanyCapability(
                    userId.toString(), "procurement.finance.post", UUID.randomUUID()))
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
