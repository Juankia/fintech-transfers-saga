package io.github.juankia.transfers.infrastructure.web;

import io.github.juankia.transfers.domain.model.Account;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(UUID id, String currency, BigDecimal balance, BigDecimal reserved,
                              BigDecimal available, String status) {

    static AccountResponse from(Account a) {
        return new AccountResponse(a.id(), a.currency().getCurrencyCode(), a.balance().amount(),
                a.reserved().amount(), a.availableBalance().amount(), a.status().name());
    }
}
