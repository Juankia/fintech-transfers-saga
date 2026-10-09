package io.github.juankia.transfers.domain.exception;

import io.github.juankia.transfers.domain.model.AccountStatus;

import java.util.UUID;

public class AccountNotActiveException extends DomainException {

    public AccountNotActiveException(UUID accountId, AccountStatus status) {
        super("Account %s is %s and cannot be operated".formatted(accountId, status));
    }
}
