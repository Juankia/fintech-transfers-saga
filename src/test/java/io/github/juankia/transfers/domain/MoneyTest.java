package io.github.juankia.transfers.domain;

import io.github.juankia.transfers.domain.exception.CurrencyMismatchException;
import io.github.juankia.transfers.domain.model.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void normalisesScaleToCurrencyMinorUnits() {
        assertThat(Money.of("10", "USD")).isEqualTo(Money.of("10.00", "USD"));
        assertThat(Money.of("10", "USD").amount().scale()).isEqualTo(2);
        assertThat(Money.of("1500", "JPY").amount().scale()).isZero();
    }

    @Test
    void rejectsMorePrecisionThanTheCurrencyAllowsInsteadOfRounding() {
        assertThatThrownBy(() -> Money.of("10.005", "USD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("more decimals");
        assertThatThrownBy(() -> Money.of("1.5", "JPY")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void arithmeticRequiresSameCurrency() {
        assertThat(Money.of("10.50", "USD").plus(Money.of("0.50", "USD"))).isEqualTo(Money.of("11", "USD"));
        assertThat(Money.of("10", "USD").minus(Money.of("12", "USD")).isNegative()).isTrue();
        assertThatThrownBy(() -> Money.of("1", "USD").plus(Money.of("1", "EUR")))
                .isInstanceOf(CurrencyMismatchException.class);
    }

    @Test
    void comparesByValue() {
        assertThat(Money.of("5", "USD").isGreaterThan(Money.of("4.99", "USD"))).isTrue();
        assertThat(Money.of("5", "USD").isLessThan(Money.of("5.00", "USD"))).isFalse();
    }
}
