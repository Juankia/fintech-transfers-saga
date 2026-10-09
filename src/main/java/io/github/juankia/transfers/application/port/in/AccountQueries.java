package io.github.juankia.transfers.application.port.in;

import io.github.juankia.transfers.application.projection.AccountStatement;
import io.github.juankia.transfers.domain.model.Account;

import java.util.UUID;

public interface AccountQueries {

    Account getAccount(UUID accountId);

    /** Served from the read model (eventually consistent with the write side). */
    AccountStatement getStatement(UUID accountId);

    boolean isOwner(UUID accountId, String principal);
}
