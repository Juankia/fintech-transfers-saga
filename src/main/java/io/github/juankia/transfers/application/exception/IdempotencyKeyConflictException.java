package io.github.juankia.transfers.application.exception;

/**
 * The Idempotency-Key was already used with a different request payload.
 */
public class IdempotencyKeyConflictException extends RuntimeException {

    public IdempotencyKeyConflictException(String key) {
        super("Idempotency-Key '%s' was already used with a different request payload".formatted(key));
    }
}
