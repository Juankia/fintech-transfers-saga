package io.github.juankia.transfers.support;

import io.github.juankia.transfers.application.port.out.AccountRepository;
import io.github.juankia.transfers.domain.model.Account;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Stores snapshots so that, like a database, callers never share mutable instances. */
public class InMemoryAccountRepository implements AccountRepository {

    private final Map<UUID, Account> rows = new HashMap<>();

    @Override
    public Optional<Account> findById(UUID id) {
        return Optional.ofNullable(rows.get(id)).map(InMemoryAccountRepository::copy);
    }

    @Override
    public Account save(Account account) {
        long next = account.version() == null ? 0 : account.version() + 1;
        Account stored = Account.rehydrate(account.id(), account.ownerId(), account.balance(), account.reserved(),
                account.status(), next);
        rows.put(account.id(), stored);
        return copy(stored);
    }

    private static Account copy(Account a) {
        return Account.rehydrate(a.id(), a.ownerId(), a.balance(), a.reserved(), a.status(), a.version());
    }
}
