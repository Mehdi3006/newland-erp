package com.newland.erp.finance.posting.api;

import com.newland.erp.finance.posting.domain.PostingException;
import java.net.URI;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = PostingController.class)
public final class PostingProblemHandler {
  @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
  public ProblemDetail unauthorized(final RuntimeException exception) {
    return problem(HttpStatus.UNAUTHORIZED, "Authentication required", exception.getMessage());
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ProblemDetail forbidden(final AccessDeniedException exception) {
    return problem(HttpStatus.FORBIDDEN, "Permission denied", exception.getMessage());
  }

  @ExceptionHandler(PostingException.class)
  public ProblemDetail conflict(final PostingException exception) {
    return problem(HttpStatus.CONFLICT, "Finance posting conflict", exception.getMessage());
  }

  @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
  public ProblemDetail invalid(final Exception exception) {
    return problem(HttpStatus.BAD_REQUEST, "Invalid finance posting request", exception.getMessage());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail integrity(final DataIntegrityViolationException exception) {
    return problem(
        HttpStatus.CONFLICT,
        "Finance posting integrity conflict",
        "The request conflicts with durable Finance posting data.");
  }

  private static ProblemDetail problem(
      final HttpStatus status, final String title, final String detail) {
    final ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(status, detail == null ? title : detail);
    problem.setTitle(title);
    problem.setType(URI.create("urn:newland:problem:finance-posting"));
    return problem;
  }
}
