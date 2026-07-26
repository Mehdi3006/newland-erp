package com.newland.erp.crm.api;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.newland.erp.crm")
public final class CrmProblemHandler {
  @ExceptionHandler(IllegalArgumentException.class)
  ProblemDetail invalid(final IllegalArgumentException exception) {
    return problem(HttpStatus.BAD_REQUEST, "Invalid CRM request", exception);
  }

  @ExceptionHandler(IllegalStateException.class)
  ProblemDetail conflict(final IllegalStateException exception) {
    return problem(HttpStatus.CONFLICT, "CRM conflict", exception);
  }

  private static ProblemDetail problem(
      final HttpStatus status, final String title, final RuntimeException exception) {
    final ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
    detail.setTitle(title);
    detail.setType(URI.create("urn:newland:problem:crm"));
    return detail;
  }
}
