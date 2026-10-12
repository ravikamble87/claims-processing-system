package com.claims.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static com.claims.TestData.money;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** "X% AFTER DEDUCTIBLE" (A4: X is the plan's share). */
class PercentAfterDeductibleRuleTest {

    private static final DeductibleStatus NOT_MET = new DeductibleStatus(false, false);
    private static final DeductibleStatus INDIVIDUAL_MET = new DeductibleStatus(true, false);
    private static final DeductibleStatus FAMILY_MET = new DeductibleStatus(false, true);
    private static final DeductibleStatus BOTH_MET = new DeductibleStatus(true, true);

    private final PercentAfterDeductibleRule fortyPercent = new PercentAfterDeductibleRule(new BigDecimal("40"));

    @Test
    void memberPaysEverythingUntilDeductibleIsMet() {
        Split split = fortyPercent.split(money("1000.00"), NOT_MET);

        assertEquals(money("1000.00"), split.memberPays());
        assertEquals(money("0.00"), split.planPays());
        assertEquals("ANNUAL DEDUCTIBLE (INDIVIDUAL or FAMILY) not met, plan pays 0%", split.processingMessage());
    }

    @Test
    void planPaysItsPercentOnceIndividualDeductibleIsMet() {
        Split split = fortyPercent.split(money("1000.00"), INDIVIDUAL_MET);

        assertEquals(money("400.00"), split.planPays());
        assertEquals(money("600.00"), split.memberPays());
        assertEquals("ANNUAL DEDUCTIBLE (INDIVIDUAL) met, plan pays 40%", split.processingMessage());
    }

    @Test
    void familyDeductibleAloneIsEnough() {
        // Sample row 14: Jina's own total is low, but the Collins family has reached $12,000.
        Split split = new PercentAfterDeductibleRule(new BigDecimal("60")).split(money("240.60"), FAMILY_MET);

        assertEquals(money("144.36"), split.planPays());
        assertEquals(money("96.24"), split.memberPays());
        assertEquals("ANNUAL DEDUCTIBLE (FAMILY) met, plan pays 60%", split.processingMessage());
    }

    @Test
    void messageNamesIndividualWhenBothAreMet() {
        // A14
        Split split = fortyPercent.split(money("500.00"), BOTH_MET);

        assertEquals("ANNUAL DEDUCTIBLE (INDIVIDUAL) met, plan pays 40%", split.processingMessage());
    }

    @Test
    void hundredPercentLeavesNothingForTheMember() {
        Split split = new PercentAfterDeductibleRule(new BigDecimal("100")).split(money("500.00"), INDIVIDUAL_MET);

        assertEquals(money("500.00"), split.planPays());
        assertEquals(money("0.00"), split.memberPays());
    }

    @ParameterizedTest
    @CsvSource({
            "33.33,   60",
            "99.99,   40",
            "0.01,    60",
            "1234.57, 70",
            "240.60,  90"
    })
    void sharesAlwaysAddUpToTheBill(BigDecimal billed, BigDecimal percent) {
        // A6: plan share is rounded, member gets the remainder, so no cent is lost or invented.
        Split split = new PercentAfterDeductibleRule(percent).split(Money.of(billed), INDIVIDUAL_MET);

        assertEquals(Money.of(billed), split.memberPays().add(split.planPays()));
    }

    @Test
    void ruleTextHasNoTrailingZeros() {
        assertEquals("40% AFTER DEDUCTIBLE", new PercentAfterDeductibleRule(new BigDecimal("40.00")).ruleText());
    }

    @Test
    void rejectsPercentOutsideZeroToHundred() {
        assertThrows(IllegalArgumentException.class, () -> new PercentAfterDeductibleRule(new BigDecimal("101")));
        assertThrows(IllegalArgumentException.class, () -> new PercentAfterDeductibleRule(new BigDecimal("-1")));
        assertThrows(NullPointerException.class, () -> new PercentAfterDeductibleRule(null));
    }
}
