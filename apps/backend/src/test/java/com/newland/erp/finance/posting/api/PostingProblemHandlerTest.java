package com.newland.erp.finance.posting.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

final class PostingProblemHandlerTest {
  private final PostingProblemHandler handler = new PostingProblemHandler();

  @Test
  void mapsMissingAuthenticationToRfc9457UnauthorizedResponse() {
    final var problem =
        handler.unauthorized(
            new AuthenticationCredentialsNotFoundException("Authentication is required."));

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(problem.getTitle()).isEqualTo("Authentication required");
    assertThat(problem.getType()).isEqualTo(URI.create("urn:newland:problem:finance-posting"));
  }

  @Test
  void mapsDeniedCompanyCapabilityToRfc9457ForbiddenResponse() {
    final var problem =
        handler.forbidden(new AccessDeniedException("Permission denied for company scope."));

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(problem.getTitle()).isEqualTo("Permission denied");
    assertThat(problem.getType()).isEqualTo(URI.create("urn:newland:problem:finance-posting"));
  }
}
