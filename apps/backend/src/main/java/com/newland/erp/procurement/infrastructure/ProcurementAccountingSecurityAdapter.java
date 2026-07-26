package com.newland.erp.procurement.infrastructure;

import com.newland.erp.identity.application.integration.IdentityAuthorizationPort;
import com.newland.erp.procurement.application.ProcurementAccountingPorts;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public final class ProcurementAccountingSecurityAdapter
    implements ProcurementAccountingPorts.SecurityPort {
  private final IdentityAuthorizationPort identity;

  public ProcurementAccountingSecurityAdapter(
      final IdentityAuthorizationPort identityAuthorizationPort) {
    identity = identityAuthorizationPort;
  }

  @Override
  public String currentActor() {
    final Authentication authentication = authentication();
    validateSession(authentication);
    return authentication.getName();
  }

  @Override
  public void requireCompanyCapability(
      final String actor, final String capability, final UUID companyId) {
    final Authentication authentication = authentication();
    if (!authentication.getName().equals(actor)) {
      throw new AuthenticationCredentialsNotFoundException(
          "Authenticated Procurement actor does not match the request.");
    }
    validateSession(authentication);
    final UUID userId = userId(actor);
    if (!identity.isCompanyCapabilityGranted(userId, capability, companyId)) {
      throw new AccessDeniedException("Procurement Finance permission denied for company scope.");
    }
  }

  private void validateSession(final Authentication authentication) {
    if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
      throw new AuthenticationCredentialsNotFoundException(
          "A session-bound JWT is required.");
    }
    final UUID userId = userId(authentication.getName());
    final UUID sessionId;
    try {
      sessionId = UUID.fromString(
          jwtAuthentication.getToken().getClaimAsString("session_id"));
    } catch (IllegalArgumentException | NullPointerException exception) {
      throw new AuthenticationCredentialsNotFoundException(
          "Authenticated Procurement session identifier is invalid.");
    }
    if (!identity.isSessionAuthorized(userId, sessionId)) {
      throw new AuthenticationCredentialsNotFoundException(
          "Authenticated Procurement session is invalid, expired, or revoked.");
    }
  }

  private static Authentication authentication() {
    final Authentication authentication =
        SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new AuthenticationCredentialsNotFoundException(
          "Authentication is required.");
    }
    return authentication;
  }

  private static UUID userId(final String actor) {
    try {
      return UUID.fromString(actor);
    } catch (IllegalArgumentException | NullPointerException exception) {
      throw new AuthenticationCredentialsNotFoundException(
          "Authenticated Procurement user identifier is invalid.");
    }
  }
}
