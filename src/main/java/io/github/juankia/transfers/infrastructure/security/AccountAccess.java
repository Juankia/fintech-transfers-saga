package io.github.juankia.transfers.infrastructure.security;

import io.github.juankia.transfers.application.port.in.AccountQueries;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Referenced from SpEL as {@code @accountAccess}: resource-level authorisation on top of scopes.
 */
@Component("accountAccess")
class AccountAccess {

    private final AccountQueries accounts;

    AccountAccess(AccountQueries accounts) {
        this.accounts = accounts;
    }

    public boolean isOwner(UUID accountId, Authentication authentication) {
        return authentication != null && accounts.isOwner(accountId, authentication.getName());
    }
}
