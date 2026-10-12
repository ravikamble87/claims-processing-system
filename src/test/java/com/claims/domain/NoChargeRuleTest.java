package com.claims.domain;

import org.junit.jupiter.api.Test;

import static com.claims.TestData.money;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Preventive care: plan pays all, member pays nothing, deductible ignored. */
class NoChargeRuleTest {

    private final NoChargeRule rule = new NoChargeRule();

    @Test
    void planPaysTheWholeBill() {
        // Sample row 15: Daniel Hayer, routine physical $350.
        Split split = rule.split(money("350.00"), new DeductibleStatus(false, false));

        assertEquals(money("350.00"), split.planPays());
        assertEquals(money("0.00"), split.memberPays());
        assertEquals("Preventive Care - No Charge to policyholder", split.processingMessage());
    }

    @Test
    void sameResultWhenDeductibleIsMet() {
        assertEquals(rule.split(money("200.00"), new DeductibleStatus(false, false)),
                rule.split(money("200.00"), new DeductibleStatus(true, true)));
    }

    @Test
    void ruleText() {
        assertEquals("No Charge", rule.ruleText());
    }
}
