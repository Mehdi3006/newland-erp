package com.newland.erp.identity.api;

import com.newland.erp.identity.domain.AccessDeniedException;
import com.newland.erp.identity.domain.AuthenticationFailedException;
import com.newland.erp.identity.domain.IdentityConflictException;
import com.newland.erp.identity.domain.IdentityNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice(basePackageClasses = IdentityController.class)
public final class IdentityProblemHandler {
    private static final URI PROBLEM_BASE = URI.create("https://newland-erp.local/problems/identity/");

    @ExceptionHandler(AuthenticationFailedException.class)
    public ProblemDetail unauthorized(final AuthenticationFailedException exception) {
        return problem(HttpStatus.UNAUTHORIZED, "Identity authentication failed", exception.getMessage(),
                "authentication-failed");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail forbidden(final AccessDeniedException exception) {
        return problem(HttpStatus.FORBIDDEN, "Identity access denied", exception.getMessage(), "access-denied");
    }

    @ExceptionHandler(IdentityNotFoundException.class)
    public ProblemDetail notFound(final IdentityNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Identity resource not found", exception.getMessage(), "not-found");
    }

    @ExceptionHandler(IdentityConflictException.class)
    public ProblemDetail conflict(final IdentityConflictException exception) {
        return problem(HttpStatus.CONFLICT, "Identity conflict", exception.getMessage(), "conflict");
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    public ProblemDetail badRequest(final Exception exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid Identity request", exception.getMessage(), "invalid-request");
    }

    private static ProblemDetail problem(final HttpStatus status, final String title, final String detail,
                                         final String type) {
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(PROBLEM_BASE.resolve(type));
        return problem;
    }
}
