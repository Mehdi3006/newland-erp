package com.newland.erp.sales.api;

import com.newland.erp.sales.domain.SalesConflictException;
import com.newland.erp.sales.domain.SalesNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = SalesController.class)
public final class SalesProblemHandler {
    @ExceptionHandler(SalesConflictException.class)
    ProblemDetail conflict(final SalesConflictException exception) {
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Sales conflict");
        return problem;
    }

    @ExceptionHandler(SalesNotFoundException.class)
    ProblemDetail notFound(final SalesNotFoundException exception) {
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Sales resource not found");
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalid(final IllegalArgumentException exception) {
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                exception.getMessage());
        problem.setTitle("Invalid sales request");
        return problem;
    }
}
