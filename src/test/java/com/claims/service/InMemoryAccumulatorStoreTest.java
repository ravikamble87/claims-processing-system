package com.claims.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.claims.TestData.money;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InMemoryAccumulatorStoreTest {

    private final InMemoryAccumulatorStore store = new InMemoryAccumulatorStore(2016,
            Map.of(1000011L, money("0.00"), 1000012L, money("0.00"), 1000031L, money("2500.00")),
            Map.of(100001L, money("0.00"), 100003L, money("2500.00")));

    @Test
    void startsFromTheOpeningValues() {
        assertEquals(money("2500.00"), store.individual(1000031));
        assertEquals(money("2500.00"), store.family(100003));
    }

    @Test
    void unknownIdsStartAtZero() {
        assertEquals(money("0.00"), store.individual(9999999));
        assertEquals(money("0.00"), store.family(999999));
    }

    @Test
    void addUpdatesBothTheMemberAndTheFamily() {
        // A3
        store.add(1000031, 100003, money("300.00"));

        assertEquals(money("2800.00"), store.individual(1000031));
        assertEquals(money("2800.00"), store.family(100003));
    }

    @Test
    void familyMembersShareOneFamilyTotal() {
        // A5: family = PolicyId
        store.add(1000011, 100001, money("6000.00"));
        store.add(1000012, 100001, money("140.00"));

        assertEquals(money("6000.00"), store.individual(1000011));
        assertEquals(money("140.00"), store.individual(1000012));
        assertEquals(money("6140.00"), store.family(100001));
    }

    @Test
    void addCreatesAccumulatorsForIdsNotSeededAtStart() {
        store.add(1000014, 100001, money("60.00"));

        assertEquals(money("60.00"), store.individual(1000014));
        assertEquals(money("60.00"), store.family(100001));
    }

    @Test
    void storesAreIndependent() {
        // A24: every run starts again from the opening values.
        store.add(1000031, 100003, money("300.00"));
        InMemoryAccumulatorStore nextRun = new InMemoryAccumulatorStore(2016,
                Map.of(1000031L, money("2500.00")), Map.of(100003L, money("2500.00")));

        assertEquals(money("2500.00"), nextRun.individual(1000031));
    }
}
