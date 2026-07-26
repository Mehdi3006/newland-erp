package com.newland.erp.crm.infrastructure;

import com.newland.erp.crm.application.CrmSecurityPort;
import com.newland.erp.identity.application.integration.IdentityAuthorizationPort;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public final class CrmSecurityAdapter implements CrmSecurityPort {
  private final IdentityAuthorizationPort identity;

  public CrmSecurityAdapter(final IdentityAuthorizationPort identityAuthorizationPort) {
    identity = identityAuthorizationPort;
  }

  @Override
  public String currentActor() {
    final var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!(authentication instanceof JwtAuthenticationToken token)
        || !authentication.isAuthenticated()) {
      throw new AuthenticationCredentialsNotFoundException("Authentication is required.");
    }
    final UUID userId = uuid(authentication.getName());
    final UUID sessionId = uuid(token.getToken().getClaimAsString("session_id"));
    if (!identity.isSessionAuthorized(userId, sessionId)) {
      throw new AuthenticationCredentialsNotFoundException("Session is invalid or revoked.");
    }
    return userId.toString();
  }

  @Override
  public void require(final String actor, final String capability, final UUID companyId) {
    final String current = currentActor();
    if (!current.equals(actor)
        || !identity.isCompanyCapabilityGranted(uuid(actor), capability, companyId)) {
      throw new AccessDeniedException("CRM permission denied for company scope.");
    }
  }

  private static UUID uuid(final String value) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException | NullPointerException exception) {
      throw new AuthenticationCredentialsNotFoundException("Invalid identity context.");
    }
  }
}
