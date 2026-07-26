package com.newland.erp.logistics.api;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.newland.erp.logistics")
public final class LogisticsProblemHandler {
  @ExceptionHandler(IllegalArgumentException.class)
  ProblemDetail invalid(final IllegalArgumentException exception) {
    return problem(HttpStatus.BAD_REQUEST, "Invalid import logistics request", exception);
  }

  @ExceptionHandler(IllegalStateException.class)
  ProblemDetail conflict(final IllegalStateException exception) {
    return problem(HttpStatus.CONFLICT, "Import logistics conflict", exception);
  }

  private static ProblemDetail problem(
      final HttpStatus status, final String title, final RuntimeException exception) {
    final ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
    detail.setTitle(title);
    detail.setType(URI.create("urn:newland:problem:import-logistics"));
    return detail;
  }
}
