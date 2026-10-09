package io.github.juankia.transfers.application.exception;

import java.util.UUID;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(UUID accountId) {
        super("Account %s not found".formatted(accountId));
    }
}
