package com.newland.erp.inventory.api;

import com.newland.erp.inventory.domain.InventoryConflictException;
import com.newland.erp.inventory.domain.InventoryException;
import com.newland.erp.inventory.domain.InventoryNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice(basePackageClasses = InventoryController.class)
public final class InventoryProblemHandler {
    private static final URI PROBLEM_BASE = URI.create("https://newland-erp.local/problems/inventory/");

    @ExceptionHandler(InventoryNotFoundException.class)
    public ProblemDetail notFound(final InventoryNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Inventory resource not found", exception.getMessage(), "not-found");
    }

    @ExceptionHandler(InventoryConflictException.class)
    public ProblemDetail conflict(final InventoryException exception) {
        return problem(HttpStatus.CONFLICT, "Inventory conflict", exception.getMessage(), "conflict");
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    public ProblemDetail badRequest(final Exception exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid Inventory request", exception.getMessage(),
                "invalid-request");
    }

    private static ProblemDetail problem(final HttpStatus status, final String title, final String detail,
                                         final String type) {
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(PROBLEM_BASE.resolve(type));
        return problem;
    }
}
