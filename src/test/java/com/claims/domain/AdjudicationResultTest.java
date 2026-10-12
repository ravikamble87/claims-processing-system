package com.claims.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.claims.TestData.claim;
import static com.claims.TestData.money;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdjudicationResultTest {

    private final Claim claim = claim("100001", "1000016", "2016-10-12", "Prescription Drugs", "GENERIC", "61.40");

    @Test
    void errorRowHasOnlyTheErrorColumns() {
        // A12, sample row 7
        AdjudicationResult result = AdjudicationResult.error(claim, ErrorCode.E0001);

        assertTrue(result.isError());
        assertEquals(ErrorCode.E0001, result.errorCode());
        assertEquals("Policy holder does not exist", result.errorMessage());
        assertNull(result.memberPays());
        assertNull(result.planPays());
        assertNull(result.ruleUsed());
        assertNull(result.individualAccumulated());
        assertNull(result.familyAccumulated());
        assertNull(result.processingMessage());
    }

    @Test
    void processedRowCarriesSplitRuleAndTotals() {
        CoverageRule rule = new PercentAfterDeductibleRule(new BigDecimal("60"));
        Split split = new Split(money("24.56"), money("36.84"), "ANNUAL DEDUCTIBLE (FAMILY) met, plan pays 60%");

        AdjudicationResult result = AdjudicationResult.processed(claim, rule, split, money("24.56"), money("12024.56"));

        assertFalse(result.isError());
        assertNull(result.errorMessage());
        assertEquals(money("24.56"), result.memberPays());
        assertEquals(money("36.84"), result.planPays());
        assertEquals("60% AFTER DEDUCTIBLE", result.ruleUsed());
        assertEquals(money("12024.56"), result.familyAccumulated());
        assertEquals(split.processingMessage(), result.processingMessage());
    }

    @Test
    void errorNeedsACode() {
        assertThrows(NullPointerException.class, () -> AdjudicationResult.error(claim, null));
    }
}
