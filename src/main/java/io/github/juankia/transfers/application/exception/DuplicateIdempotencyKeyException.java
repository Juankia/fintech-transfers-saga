package io.github.juankia.transfers.application.exception;

/**
 * Raised by the idempotency store when a concurrent request registered the same key first.
 */
public class DuplicateIdempotencyKeyException extends RuntimeException {

    public DuplicateIdempotencyKeyException(String key, Throwable cause) {
        super("Idempotency-Key '%s' is already registered".formatted(key), cause);
    }
}
