package com.newland.erp.enterprise.api;

import com.newland.erp.enterprise.domain.AuthorizationDeniedException;
import com.newland.erp.enterprise.domain.DuplicateBusinessCodeException;
import com.newland.erp.enterprise.domain.EnterpriseStructureException;
import com.newland.erp.enterprise.domain.InactiveParentException;
import com.newland.erp.enterprise.domain.InvalidStateTransitionException;
import com.newland.erp.enterprise.domain.NotFoundException;
import com.newland.erp.enterprise.domain.OptimisticLockConflictException;
import com.newland.erp.enterprise.domain.ReferencedByActiveChildrenException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice(basePackageClasses = EnterpriseStructureController.class)
public final class EnterpriseStructureProblemHandler {
    private static final URI PROBLEM_BASE = URI.create("https://newland-erp.local/problems/enterprise-structure/");

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail notFound(final NotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Enterprise Structure resource not found", exception.getMessage(),
                "not-found");
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ProblemDetail forbidden(final AuthorizationDeniedException exception) {
        return problem(HttpStatus.FORBIDDEN, "Enterprise Structure permission denied", exception.getMessage(),
                "permission-denied");
    }

    @ExceptionHandler({
        DuplicateBusinessCodeException.class,
        InactiveParentException.class,
        InvalidStateTransitionException.class,
        OptimisticLockConflictException.class,
        ReferencedByActiveChildrenException.class,
    })
    public ProblemDetail conflict(final EnterpriseStructureException exception) {
        return problem(HttpStatus.CONFLICT, "Enterprise Structure conflict", exception.getMessage(), "conflict");
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    public ProblemDetail badRequest(final Exception exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid Enterprise Structure request", exception.getMessage(),
                "invalid-request");
    }

    private static ProblemDetail problem(
            final HttpStatus status,
            final String title,
            final String detail,
            final String type
    ) {
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(PROBLEM_BASE.resolve(type));
        return problem;
    }
}
