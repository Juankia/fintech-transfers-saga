package io.github.juankia.transfers.infrastructure.web;

import io.github.juankia.transfers.application.exception.AccountAccessDeniedException;
import io.github.juankia.transfers.application.exception.AccountNotFoundException;
import io.github.juankia.transfers.application.exception.IdempotencyKeyConflictException;
import io.github.juankia.transfers.application.exception.TransferNotFoundException;
import io.github.juankia.transfers.domain.exception.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Maps application errors to RFC 9457 problem details.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({AccountNotFoundException.class, TransferNotFoundException.class})
    ProblemDetail notFound(RuntimeException e) {
        return problem(HttpStatus.NOT_FOUND, "Not found", e);
    }

    @ExceptionHandler(AccountAccessDeniedException.class)
    ProblemDetail forbidden(AccountAccessDeniedException e) {
        return problem(HttpStatus.FORBIDDEN, "Forbidden", e);
    }

    @ExceptionHandler(IdempotencyKeyConflictException.class)
    ProblemDetail idempotencyConflict(IdempotencyKeyConflictException e) {
        return problem(HttpStatus.CONFLICT, "Idempotency-Key reused", e);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail concurrentModification(ObjectOptimisticLockingFailureException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The resource was modified concurrently; retry with the same Idempotency-Key");
        pd.setTitle("Concurrent modification");
        return pd;
    }

    @ExceptionHandler(TransferController.InvalidIdempotencyKeyException.class)
    ProblemDetail invalidKey(RuntimeException e) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid Idempotency-Key", e);
    }

    @ExceptionHandler({DomainException.class, IllegalArgumentException.class})
    ProblemDetail unprocessable(RuntimeException e) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid transfer", e);
    }

    private static ProblemDetail problem(HttpStatus status, String title, Exception e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, e.getMessage());
        pd.setTitle(title);
        return pd;
    }
}
