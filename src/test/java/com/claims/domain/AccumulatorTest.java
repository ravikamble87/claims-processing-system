package com.claims.domain;

import org.junit.jupiter.api.Test;

import static com.claims.TestData.money;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccumulatorTest {

    @Test
    void addReturnsANewValueAndLeavesTheOriginalAlone() {
        Accumulator before = new Accumulator(1000011, AccumulatorScope.INDIVIDUAL, 2016, money("6000.00"));

        Accumulator after = before.add(money("600.00"));

        assertEquals(money("6600.00"), after.amount());
        assertEquals(money("6000.00"), before.amount());
        assertEquals(before.ownerId(), after.ownerId());
        assertEquals(before.scope(), after.scope());
        assertEquals(before.benefitYear(), after.benefitYear());
    }

    @Test
    void amountIsAlwaysTwoDecimals() {
        assertEquals(money("4000.00"),
                new Accumulator(1000021, AccumulatorScope.INDIVIDUAL, 2016, money("4000")).amount());
        assertEquals(money("0.00"), Accumulator.zero(100001, AccumulatorScope.FAMILY, 2016).amount());
    }

    @Test
    void addingZeroKeepsTheTotal() {
        // A No Charge claim adds 0.00 for the member.
        Accumulator total = new Accumulator(1000031, AccumulatorScope.INDIVIDUAL, 2016, money("2500.00"));

        assertEquals(money("2500.00"), total.add(Money.ZERO).amount());
    }

    @Test
    void rejectsMissingValues() {
        assertThrows(NullPointerException.class, () -> new Accumulator(1, null, 2016, money("0.00")));
        assertThrows(NullPointerException.class, () -> new Accumulator(1, AccumulatorScope.FAMILY, 2016, null));
    }
}
