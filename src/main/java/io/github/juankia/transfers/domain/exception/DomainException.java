package io.github.juankia.transfers.domain.exception;

/**
 * Base type for business-rule violations raised by aggregates and value objects.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
