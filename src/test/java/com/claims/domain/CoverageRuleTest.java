package com.claims.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static com.claims.TestData.money;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** CoverageRule.parse: the three rule formats in coverage.csv, and nothing else. */
class CoverageRuleTest {

    @ParameterizedTest
    @CsvSource({
            "40% AFTER DEDUCTIBLE,         40% AFTER DEDUCTIBLE",
            "'  60%   after deductible  ', 60% AFTER DEDUCTIBLE",
            "100% AFTER DEDUCTIBLE,        100% AFTER DEDUCTIBLE"
    })
    void parsesPercentRule(String text, String expectedRuleText) {
        CoverageRule rule = CoverageRule.parse(text);

        assertInstanceOf(PercentAfterDeductibleRule.class, rule);
        assertEquals(expectedRuleText, rule.ruleText());
    }

    @ParameterizedTest
    @ValueSource(strings = {"No Charge", "NO CHARGE", " no   charge "})
    void parsesNoChargeRule(String text) {
        CoverageRule rule = CoverageRule.parse(text);

        assertInstanceOf(NoChargeRule.class, rule);
        assertEquals("No Charge", rule.ruleText());
    }

    @ParameterizedTest
    @CsvSource({
            "$120 FLAT, 120.00, $120 FLAT",
            "$100,      100.00, $100 FLAT",
            "120,       120.00, $120 FLAT"    // the workbook stores flat amounts as bare numbers
    })
    void parsesFlatRule(String text, String expectedAmount, String expectedRuleText) {
        CoverageRule rule = CoverageRule.parse(text);

        FlatAmountRule flat = assertInstanceOf(FlatAmountRule.class, rule);
        assertEquals(money(expectedAmount), flat.planAmount());
        assertEquals(expectedRuleText, rule.ruleText());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "50% BEFORE DEDUCTIBLE", "FREE", "40%", "$abc FLAT"})
    void rejectsUnknownRuleText(String text) {
        assertThrows(IllegalArgumentException.class, () -> CoverageRule.parse(text));
    }

    @Test
    void rejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> CoverageRule.parse(null));
    }
}
