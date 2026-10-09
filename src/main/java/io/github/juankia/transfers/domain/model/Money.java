package io.github.juankia.transfers.domain.model;

import io.github.juankia.transfers.domain.exception.CurrencyMismatchException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Immutable monetary amount. The scale is normalised to the currency's minor units
 * (e.g. 2 for USD, 0 for JPY) and amounts with more precision are rejected rather than
 * silently rounded: rounding money is a business decision, never an accident.
 */
public record Money(BigDecimal amount, Currency currency) implements Comparable<Money> {

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        try {
            amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Amount %s has more decimals than %s allows"
                    .formatted(amount.toPlainString(), currency.getCurrencyCode()));
        }
    }

    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(amount, Currency.getInstance(currencyCode));
    }

    public static Money of(String amount, String currencyCode) {
        return of(new BigDecimal(amount), currencyCode);
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money plus(Money other) {
        assertSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money minus(Money other) {
        assertSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isGreaterThan(Money other) {
        return compareTo(other) > 0;
    }

    public boolean isLessThan(Money other) {
        return compareTo(other) < 0;
    }

    public void assertSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new CurrencyMismatchException(currency, other.currency);
        }
    }

    @Override
    public int compareTo(Money other) {
        assertSameCurrency(other);
        return amount.compareTo(other.amount);
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency.getCurrencyCode();
    }
}
