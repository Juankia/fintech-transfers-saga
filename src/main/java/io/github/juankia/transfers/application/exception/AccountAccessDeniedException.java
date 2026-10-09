package io.github.juankia.transfers.application.exception;

import java.util.UUID;

public class AccountAccessDeniedException extends RuntimeException {

    public AccountAccessDeniedException(UUID accountId) {
        super("Caller is not allowed to operate account %s".formatted(accountId));
    }
}
