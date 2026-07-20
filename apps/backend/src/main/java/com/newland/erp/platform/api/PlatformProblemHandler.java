package com.newland.erp.platform.api;

import com.newland.erp.platform.domain.PlatformConflictException;
import com.newland.erp.platform.domain.PlatformNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice(basePackageClasses = PlatformController.class)
public final class PlatformProblemHandler {
    private static final URI PROBLEM_BASE = URI.create("https://newland-erp.local/problems/platform/");

    @ExceptionHandler(PlatformNotFoundException.class)
    public ProblemDetail notFound(final PlatformNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Platform resource not found", exception.getMessage(), "not-found");
    }

    @ExceptionHandler(PlatformConflictException.class)
    public ProblemDetail conflict(final PlatformConflictException exception) {
        return problem(HttpStatus.CONFLICT, "Platform conflict", exception.getMessage(), "conflict");
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    public ProblemDetail badRequest(final Exception exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid Platform request", exception.getMessage(), "invalid-request");
    }

    private static ProblemDetail problem(final HttpStatus status, final String title, final String detail,
                                         final String type) {
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(PROBLEM_BASE.resolve(type));
        return problem;
    }
}
