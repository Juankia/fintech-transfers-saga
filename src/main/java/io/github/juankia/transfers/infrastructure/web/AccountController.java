package io.github.juankia.transfers.infrastructure.web;

import io.github.juankia.transfers.application.port.in.AccountQueries;
import io.github.juankia.transfers.application.projection.AccountStatement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "accounts")
class AccountController {

    private final AccountQueries accounts;

    AccountController(AccountQueries accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/{accountId}")
    @PreAuthorize("hasAuthority('SCOPE_accounts:read') and @accountAccess.isOwner(#accountId, authentication)")
    @Operation(summary = "Current balance (write model, strongly consistent)")
    AccountResponse get(@PathVariable UUID accountId) {
        return AccountResponse.from(accounts.getAccount(accountId));
    }

    @GetMapping("/{accountId}/statement")
    @PreAuthorize("hasAuthority('SCOPE_accounts:read') and @accountAccess.isOwner(#accountId, authentication)")
    @Operation(summary = "Account statement (CQRS read model, eventually consistent)")
    AccountStatement statement(@PathVariable UUID accountId) {
        return accounts.getStatement(accountId);
    }
}
