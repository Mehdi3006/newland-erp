package com.newland.erp.masterdata.api;

import com.newland.erp.masterdata.domain.DuplicateMasterDataCodeException;
import com.newland.erp.masterdata.domain.MasterDataException;
import com.newland.erp.masterdata.domain.MasterDataNotFoundException;
import com.newland.erp.masterdata.domain.MasterDataVersionConflictException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice(basePackageClasses = MasterDataController.class)
public final class MasterDataProblemHandler {
    private static final URI PROBLEM_BASE = URI.create("https://newland-erp.local/problems/master-data/");

    @ExceptionHandler(MasterDataNotFoundException.class)
    public ProblemDetail notFound(final MasterDataNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Master Data resource not found", exception.getMessage(), "not-found");
    }

    @ExceptionHandler({
        DuplicateMasterDataCodeException.class,
        MasterDataVersionConflictException.class,
    })
    public ProblemDetail conflict(final MasterDataException exception) {
        return problem(HttpStatus.CONFLICT, "Master Data conflict", exception.getMessage(), "conflict");
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    public ProblemDetail badRequest(final Exception exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid Master Data request", exception.getMessage(),
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
