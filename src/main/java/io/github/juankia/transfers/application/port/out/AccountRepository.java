package io.github.juankia.transfers.application.port.out;

import io.github.juankia.transfers.domain.model.Account;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {

    Optional<Account> findById(UUID id);

    /** Persists the aggregate, enforcing optimistic locking on its version. */
    Account save(Account account);
}
