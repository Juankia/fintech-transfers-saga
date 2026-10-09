package io.github.juankia.transfers.domain;

import io.github.juankia.transfers.domain.exception.AccountNotActiveException;
import io.github.juankia.transfers.domain.exception.CurrencyMismatchException;
import io.github.juankia.transfers.domain.exception.InsufficientFundsException;
import io.github.juankia.transfers.domain.model.Account;
import io.github.juankia.transfers.domain.model.Money;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    private final Account account = Account.open(UUID.randomUUID(), "alice", usd("100"));

    @Test
    void reservationLowersAvailableBalanceButNotLedgerBalance() {
        account.reserve(usd("30"));

        assertThat(account.balance()).isEqualTo(usd("100"));
        assertThat(account.reserved()).isEqualTo(usd("30"));
        assertThat(account.availableBalance()).isEqualTo(usd("70"));
    }

    @Test
    void cannotReserveMoreThanAvailable() {
        account.reserve(usd("80"));

        assertThatThrownBy(() -> account.reserve(usd("20.01")))
                .isInstanceOf(InsufficientFundsException.class);
        assertThat(account.reserved()).isEqualTo(usd("80"));
    }

    @Test
    void debitConsumesReservation() {
        account.reserve(usd("30"));
        account.debitReserved(usd("30"));

        assertThat(account.balance()).isEqualTo(usd("70"));
        assertThat(account.reserved()).isEqualTo(usd("0"));
    }

    @Test
    void cannotDebitWithoutReservation() {
        assertThatThrownBy(() -> account.debitReserved(usd("10"))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void frozenAccountRejectsOperationsButAcceptsCompensations() {
        account.reserve(usd("40"));
        account.freeze();

        assertThatThrownBy(() -> account.credit(usd("1"))).isInstanceOf(AccountNotActiveException.class);
        assertThatThrownBy(() -> account.debitReserved(usd("40"))).isInstanceOf(AccountNotActiveException.class);

        account.releaseReservation(usd("40"));
        account.refund(usd("5"));

        assertThat(account.reserved()).isEqualTo(usd("0"));
        assertThat(account.balance()).isEqualTo(usd("105"));
    }

    @Test
    void rejectsForeignCurrency() {
        assertThatThrownBy(() -> account.credit(Money.of("10", "EUR"))).isInstanceOf(CurrencyMismatchException.class);
    }

    private static Money usd(String amount) {
        return Money.of(amount, "USD");
    }
}
