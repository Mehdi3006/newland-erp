package com.newland.erp.procurement.api;

import com.newland.erp.procurement.domain.ProcurementConflictException;
import com.newland.erp.procurement.domain.ProcurementNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ProcurementController.class)
public final class ProcurementProblemHandler {
    @ExceptionHandler(ProcurementConflictException.class)
    ProblemDetail conflict(final ProcurementConflictException exception) {
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Procurement conflict");
        return problem;
    }

    @ExceptionHandler(ProcurementNotFoundException.class)
    ProblemDetail notFound(final ProcurementNotFoundException exception) {
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Procurement resource not found");
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalid(final IllegalArgumentException exception) {
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                exception.getMessage());
        problem.setTitle("Invalid procurement request");
        return problem;
    }
}
