package com.newland.erp.productcatalog.api;

import com.newland.erp.productcatalog.domain.DuplicateProductIdentifierException;
import com.newland.erp.productcatalog.domain.ProductException;
import com.newland.erp.productcatalog.domain.ProductNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice(basePackageClasses = ProductCatalogController.class)
public final class ProductCatalogProblemHandler {
    private static final URI PROBLEM_BASE = URI.create("https://newland-erp.local/problems/product-catalog/");

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail notFound(final ProductNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Product Catalog resource not found", exception.getMessage(),
                "not-found");
    }

    @ExceptionHandler(DuplicateProductIdentifierException.class)
    public ProblemDetail conflict(final ProductException exception) {
        return problem(HttpStatus.CONFLICT, "Product Catalog conflict", exception.getMessage(), "conflict");
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    public ProblemDetail badRequest(final Exception exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid Product Catalog request", exception.getMessage(),
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
