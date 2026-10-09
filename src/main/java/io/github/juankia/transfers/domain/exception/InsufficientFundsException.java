package io.github.juankia.transfers.domain.exception;

import io.github.juankia.transfers.domain.model.Money;

import java.util.UUID;

public class InsufficientFundsException extends DomainException {

    public InsufficientFundsException(UUID accountId, Money requested, Money available) {
        super("Account %s has insufficient funds: requested %s, available %s"
                .formatted(accountId, requested, available));
    }
}
