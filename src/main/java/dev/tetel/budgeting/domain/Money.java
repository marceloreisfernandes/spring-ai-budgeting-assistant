package dev.tetel.budgeting.domain;

import java.math.BigDecimal;

/**
 * Monetary amount expressed in integer cents. Never use {@code double}/{@code float} for money;
 * {@link BigDecimal} is used only when presenting the amount in currency form.
 */
public record Money(long cents) {

    /** Documented domain upper limit for this educational project: 1,000,000.00 in the currency unit. */
    public static final long MAX_AMOUNT_CENTS = 100_000_000L;

    public Money {
        if (cents <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (cents > MAX_AMOUNT_CENTS) {
            throw new IllegalArgumentException("Amount exceeds the maximum allowed value of " + MAX_AMOUNT_CENTS + " cents");
        }
    }

    public static Money ofCents(long cents) {
        return new Money(cents);
    }

    public BigDecimal toDecimal() {
        return BigDecimal.valueOf(cents, 2);
    }
}
