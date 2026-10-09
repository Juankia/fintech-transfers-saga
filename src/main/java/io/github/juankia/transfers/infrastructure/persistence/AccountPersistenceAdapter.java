package io.github.juankia.transfers.infrastructure.persistence;

import io.github.juankia.transfers.application.port.out.AccountRepository;
import io.github.juankia.transfers.domain.model.Account;
import io.github.juankia.transfers.domain.model.Money;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
class AccountPersistenceAdapter implements AccountRepository {

    private final AccountJpaRepository repository;

    AccountPersistenceAdapter(AccountJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return repository.findById(id).map(AccountPersistenceAdapter::toDomain);
    }

    @Override
    public Account save(Account account) {
        // A detached entity carrying the aggregate's version: merge() fails with an optimistic
        // locking exception if somebody else changed the row in the meantime.
        AccountEntity entity = new AccountEntity(account.id(), account.ownerId(),
                account.currency().getCurrencyCode(), account.balance().amount(), account.reserved().amount(),
                account.status(), account.version());
        return toDomain(repository.save(entity));
    }

    private static Account toDomain(AccountEntity e) {
        return Account.rehydrate(e.getId(), e.getOwnerId(),
                Money.of(e.getBalance(), e.getCurrency()), Money.of(e.getReserved(), e.getCurrency()),
                e.getStatus(), e.getVersion());
    }
}
