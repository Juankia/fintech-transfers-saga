package io.github.juankia.transfers.domain.model;

import io.github.juankia.transfers.domain.exception.AccountNotActiveException;
import io.github.juankia.transfers.domain.exception.CurrencyMismatchException;
import io.github.juankia.transfers.domain.exception.InsufficientFundsException;
import io.github.juankia.transfers.domain.exception.InvalidTransferException;

import java.util.Currency;
import java.util.Objects;
import java.util.UUID;

/**
 * Account aggregate. Funds move in two phases: a <em>reservation</em> (hold) that lowers the
 * available balance, followed by a debit that consumes the reservation. Compensating operations
 * ({@link #releaseReservation(Money)} and {@link #refund(Money)}) deliberately skip the
 * "account must be active" guard: a compensation must always be able to complete, even if the
 * account was frozen while the saga was in flight.
 */
public final class Account {

    private final UUID id;
    private final String ownerId;
    private final Currency currency;
    private Money balance;
    private Money reserved;
    private AccountStatus status;
    private final Long version;

    private Account(UUID id, String ownerId, Money balance, Money reserved, AccountStatus status, Long version) {
        this.id = Objects.requireNonNull(id, "id");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
        this.currency = balance.currency();
        balance.assertSameCurrency(reserved);
        this.balance = balance;
        this.reserved = reserved;
        this.status = Objects.requireNonNull(status, "status");
        this.version = version;
    }

    public static Account open(UUID id, String ownerId, Money openingBalance) {
        if (openingBalance.isNegative()) {
            throw new InvalidTransferException("Opening balance cannot be negative");
        }
        return new Account(id, ownerId, openingBalance, Money.zero(openingBalance.currency()), AccountStatus.ACTIVE, null);
    }

    public static Account rehydrate(UUID id, String ownerId, Money balance, Money reserved,
                                    AccountStatus status, Long version) {
        return new Account(id, ownerId, balance, reserved, status, version);
    }

    public Money availableBalance() {
        return balance.minus(reserved);
    }

    /** Saga step 1: place a hold on funds. */
    public void reserve(Money amount) {
        ensureActive();
        ensureValidAmount(amount);
        if (availableBalance().isLessThan(amount)) {
            throw new InsufficientFundsException(id, amount, availableBalance());
        }
        reserved = reserved.plus(amount);
    }

    /** Compensation for {@link #reserve(Money)}. */
    public void releaseReservation(Money amount) {
        ensureValidAmount(amount);
        if (reserved.isLessThan(amount)) {
            throw new IllegalStateException("Cannot release %s, only %s reserved on account %s"
                    .formatted(amount, reserved, id));
        }
        reserved = reserved.minus(amount);
    }

    /** Saga step 3: consume a previous reservation. */
    public void debitReserved(Money amount) {
        ensureActive();
        ensureValidAmount(amount);
        if (reserved.isLessThan(amount)) {
            throw new IllegalStateException("Cannot debit %s, only %s reserved on account %s"
                    .formatted(amount, reserved, id));
        }
        reserved = reserved.minus(amount);
        balance = balance.minus(amount);
    }

    /** Saga step 4: credit the beneficiary. */
    public void credit(Money amount) {
        ensureActive();
        ensureValidAmount(amount);
        balance = balance.plus(amount);
    }

    /** Compensation for {@link #debitReserved(Money)}: give the money back to the payer. */
    public void refund(Money amount) {
        ensureValidAmount(amount);
        balance = balance.plus(amount);
    }

    public void freeze() {
        status = AccountStatus.FROZEN;
    }

    public boolean isOwnedBy(String candidateOwner) {
        return ownerId.equals(candidateOwner);
    }

    private void ensureActive() {
        if (status != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException(id, status);
        }
    }

    private void ensureValidAmount(Money amount) {
        if (!currency.equals(amount.currency())) {
            throw new CurrencyMismatchException(currency, amount.currency());
        }
        if (!amount.isPositive()) {
            throw new InvalidTransferException("Amount must be positive");
        }
    }

    public UUID id() { return id; }
    public String ownerId() { return ownerId; }
    public Currency currency() { return currency; }
    public Money balance() { return balance; }
    public Money reserved() { return reserved; }
    public AccountStatus status() { return status; }
    public Long version() { return version; }
}
