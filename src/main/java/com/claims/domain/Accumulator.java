package com.claims.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Year-to-date amount the member (or whole family) has paid themselves.
 * Immutable: add() returns a new value, and the store that holds accumulators decides how to save it.
 * The member's share is ALWAYS added, even after the deductible is met (assumption A3).
 */
public record Accumulator(long ownerId, AccumulatorScope scope, int benefitYear, BigDecimal amount) {

    public Accumulator {
        Objects.requireNonNull(scope, "scope");
        amount = Money.of(Objects.requireNonNull(amount, "amount"));
    }

    public static Accumulator zero(long ownerId, AccumulatorScope scope, int benefitYear) {
        return new Accumulator(ownerId, scope, benefitYear, Money.ZERO);
    }

    public Accumulator add(BigDecimal memberPays) {
        return new Accumulator(ownerId, scope, benefitYear, amount.add(memberPays));
    }
}
