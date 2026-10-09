package io.github.juankia.transfers.domain.exception;

import java.util.Currency;

public class CurrencyMismatchException extends DomainException {

    public CurrencyMismatchException(Currency expected, Currency actual) {
        super("Currency mismatch: expected %s but got %s".formatted(expected, actual));
    }
}
