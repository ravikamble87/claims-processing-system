package com.claims.domain;

import org.junit.jupiter.api.Test;

import static com.claims.TestData.money;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** "$N FLAT": plan pays N whatever the deductible says; never more than the bill (A7). */
class FlatAmountRuleTest {

    private final FlatAmountRule p003UrgentCare = new FlatAmountRule(money("120"));

    @Test
    void planPaysTheFlatAmountAndMemberPaysTheRest() {
        // Sample row 18: Mack Lee, urgent care $250 on P003.
        Split split = p003UrgentCare.split(money("250.00"), new DeductibleStatus(false, false));

        assertEquals(money("120.00"), split.planPays());
        assertEquals(money("130.00"), split.memberPays());
        assertEquals("Flat dollar coverage - plan pays $120 regardless of deductible", split.processingMessage());
    }

    @Test
    void deductibleStatusDoesNotChangeTheSplit() {
        Split notMet = p003UrgentCare.split(money("400.00"), new DeductibleStatus(false, false));
        Split met = p003UrgentCare.split(money("400.00"), new DeductibleStatus(true, true));

        assertEquals(notMet, met);
    }

    @Test
    void planNeverPaysMoreThanTheBill() {
        Split split = new FlatAmountRule(money("100")).split(money("80.00"), new DeductibleStatus(false, false));

        assertEquals(money("80.00"), split.planPays());
        assertEquals(money("0.00"), split.memberPays());
    }

    @Test
    void billEqualToFlatAmountIsFullyPaid() {
        Split split = new FlatAmountRule(money("100")).split(money("100.00"), new DeductibleStatus(false, false));

        assertEquals(money("100.00"), split.planPays());
        assertEquals(money("0.00"), split.memberPays());
    }

    @Test
    void ruleTextMatchesExpectedOutput() {
        assertEquals("$120 FLAT", p003UrgentCare.ruleText());
        assertEquals("$100 FLAT", new FlatAmountRule(money("100.00")).ruleText());
    }

    @Test
    void rejectsNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> new FlatAmountRule(money("-1")));
    }
}
