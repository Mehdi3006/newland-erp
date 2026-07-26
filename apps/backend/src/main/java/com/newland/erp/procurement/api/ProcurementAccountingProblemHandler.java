package com.newland.erp.procurement.api;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = ProcurementAccountingController.class)
public final class ProcurementAccountingProblemHandler {
  @ExceptionHandler(IllegalArgumentException.class)
  ProblemDetail badRequest(final IllegalArgumentException exception) {
    return problem(HttpStatus.BAD_REQUEST, "PROCUREMENT_ACCOUNTING_INVALID", exception);
  }

  @ExceptionHandler(IllegalStateException.class)
  ProblemDetail conflict(final IllegalStateException exception) {
    return problem(HttpStatus.CONFLICT, "PROCUREMENT_ACCOUNTING_CONFLICT", exception);
  }

  @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
  ProblemDetail unauthenticated(
      final AuthenticationCredentialsNotFoundException exception) {
    return problem(HttpStatus.UNAUTHORIZED, "PROCUREMENT_ACCOUNTING_UNAUTHENTICATED", exception);
  }

  @ExceptionHandler(AccessDeniedException.class)
  ProblemDetail forbidden(final AccessDeniedException exception) {
    return problem(HttpStatus.FORBIDDEN, "PROCUREMENT_ACCOUNTING_FORBIDDEN", exception);
  }

  private static ProblemDetail problem(
      final HttpStatus status, final String code, final RuntimeException exception) {
    final ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
    detail.setTitle(status.getReasonPhrase());
    detail.setType(URI.create("urn:newland:problem:" + code.toLowerCase()));
    detail.setProperty("code", code);
    return detail;
  }
}
