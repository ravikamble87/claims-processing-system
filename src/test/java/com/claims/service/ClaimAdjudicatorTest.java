package com.claims.service;

import com.claims.TestData;
import com.claims.domain.AdjudicationResult;
import com.claims.domain.Claim;
import com.claims.domain.ErrorCode;
import com.claims.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.claims.TestData.claim;
import static com.claims.TestData.money;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end rule checks for one or a few claims at a time, with accumulators carried between claims
 * the same way the batch job does. Each test starts from the PolicyData opening values.
 */
class ClaimAdjudicatorTest {

    private final ClaimAdjudicator adjudicator = TestData.adjudicator();
    private AccumulatorStore accumulators;

    @BeforeEach
    void freshAccumulators() {
        accumulators = TestData.referenceData().newAccumulatorStore(TestData.BENEFIT_YEAR);
    }

    @Test
    void deductibleNotMetMemberPaysAllAndBothTotalsGrow() {
        // Sally Adams, P001, starts at 2,500 / 2,500
        AdjudicationResult result = run(claim("100003", "1000031", "2016-03-10",
                "Outpatient Services", "PRIMARY CARE OFFICE VISIT", "300.00"));

        assertPaid(result, "300.00", "0.00");
        assertEquals("40% AFTER DEDUCTIBLE", result.ruleUsed());
        assertEquals(money("2800.00"), result.individualAccumulated());
        assertEquals(money("2800.00"), result.familyAccumulated());
        assertEquals("ANNUAL DEDUCTIBLE (INDIVIDUAL or FAMILY) not met, plan pays 0%", result.processingMessage());
    }

    @Test
    void claimThatCrossesTheDeductibleIsNotSplit() {
        // A1: Sally at 2,800 has a 3,500 surgery. Status is checked BEFORE the claim, so she pays all of it.
        run(claim("100003", "1000031", "2016-03-10", "Outpatient Services", "PRIMARY CARE OFFICE VISIT", "300.00"));

        AdjudicationResult crossing = run(claim("100003", "1000031", "2016-03-15",
                "Outpatient Services", "OUTPATIENT SURGERY", "3500.00"));
        AdjudicationResult next = run(claim("100003", "1000031", "2016-03-20",
                "Outpatient Services", "LAB TESTS", "1000.00"));

        assertPaid(crossing, "3500.00", "0.00");
        assertEquals(money("6300.00"), crossing.individualAccumulated());
        assertPaid(next, "200.00", "800.00");
        assertEquals("ANNUAL DEDUCTIBLE (INDIVIDUAL) met, plan pays 80%", next.processingMessage());
    }

    @Test
    void exactlyAtTheDeductibleCountsAsMet() {
        // A2: San Mildred 3,250.61 + 2,749.39 = 6,000.00 exactly
        AdjudicationResult first = run(claim("100006", "1000061", "2016-02-01",
                "Outpatient Services", "MRI", "2749.39"));
        AdjudicationResult second = run(claim("100006", "1000061", "2016-02-02",
                "Outpatient Services", "X-RAYS", "500.00"));

        assertPaid(first, "2749.39", "0.00");
        assertEquals(money("6000.00"), first.individualAccumulated());
        assertPaid(second, "300.00", "200.00");
    }

    @Test
    void familyMetButIndividualNot() {
        // Jack Collins has paid nothing himself, but the Collins family has reached 12,000.
        accumulators = new InMemoryAccumulatorStore(TestData.BENEFIT_YEAR,
                Map.of(1000014L, money("0.00")), Map.of(100001L, money("12000.00")));

        AdjudicationResult result = run(claim("100001", "1000014", "2016-09-01",
                "Outpatient Services", "SPECIALTY CARE OFFICE VISIT", "200.00"));

        assertPaid(result, "60.00", "140.00");
        assertEquals("ANNUAL DEDUCTIBLE (FAMILY) met, plan pays 70%", result.processingMessage());
        assertEquals(money("60.00"), result.individualAccumulated());
        assertEquals(money("12060.00"), result.familyAccumulated());
    }

    @Test
    void memberShareKeepsCountingAfterTheDeductibleIsMet() {
        // A3: Tom Haskel starts at 6,712.34, already over 6,000
        AdjudicationResult result = run(claim("100005", "1000051", "2016-04-01",
                "Outpatient Services", "SPECIALTY CARE OFFICE VISIT", "500.00"));

        assertPaid(result, "150.00", "350.00");
        assertEquals(money("6862.34"), result.individualAccumulated());
        assertEquals(money("6862.34"), result.familyAccumulated());
    }

    @Test
    void sameServiceDifferentMainCategoryUsesDifferentRule() {
        // P001: inpatient lab tests 40%, outpatient lab tests 80%
        AdjudicationResult inpatient = run(claim("100005", "1000051", "2016-04-03",
                "Inpatient Hospital Care", "LAB TESTS", "1000.00"));
        AdjudicationResult outpatient = run(claim("100005", "1000051", "2016-04-04",
                "Outpatient Services", "LAB TESTS", "1000.00"));

        assertPaid(inpatient, "600.00", "400.00");
        assertPaid(outpatient, "200.00", "800.00");
    }

    @Test
    void noChargeLeavesTheTotalsAsTheyWere() {
        AdjudicationResult result = run(claim("100003", "1000031", "2016-04-05",
                "Preventive Care", "MAMMOGRAMS", "450.00"));

        assertPaid(result, "0.00", "450.00");
        assertEquals("No Charge", result.ruleUsed());
        assertEquals(money("2500.00"), result.individualAccumulated());
        assertEquals(money("2500.00"), result.familyAccumulated());
    }

    @Test
    void flatAmountMemberShareCountsTowardsTheDeductible() {
        // Sample row 18: Mack Lee 4,460.82 + 130 = 4,590.82
        AdjudicationResult result = run(claim("100007", "1000071", "2016-07-10",
                "Emergency And Urgent Care", "URGENT CARE VISIT", "250.00"));

        assertPaid(result, "130.00", "120.00");
        assertEquals("$120 FLAT", result.ruleUsed());
        assertEquals(money("4590.82"), result.individualAccumulated());
    }

    @Test
    void flatAmountIsCappedAtTheBill() {
        // A7: Daniel Hayer, P002 pays $100 flat; the bill is only $80
        AdjudicationResult result = run(claim("100002", "1000021", "2016-08-21",
                "Emergency And Urgent Care", "URGENT CARE VISIT", "80.00"));

        assertPaid(result, "0.00", "80.00");
        assertEquals("$100 FLAT", result.ruleUsed());
        assertEquals(money("4000.00"), result.individualAccumulated());
    }

    @Test
    void sameServiceIsFlatOnOnePlanAndPercentOnAnother() {
        // Urgent care: P001 40% after deductible, P002 $100 flat
        AdjudicationResult p001 = run(claim("100005", "1000051", "2016-04-02",
                "Emergency And Urgent Care", "URGENT CARE VISIT", "300.00"));
        AdjudicationResult p002 = run(claim("100002", "1000021", "2016-08-20",
                "Emergency And Urgent Care", "URGENT CARE VISIT", "250.00"));

        assertPaid(p001, "180.00", "120.00");
        assertEquals("40% AFTER DEDUCTIBLE", p001.ruleUsed());
        assertPaid(p002, "150.00", "100.00");
        assertEquals("$100 FLAT", p002.ruleUsed());
    }

    @Test
    void roundingKeepsTheSharesEqualToTheBill() {
        // A6: 33.33 x 60% = 19.998 -> plan 20.00, member 13.33
        AdjudicationResult result = run(claim("100005", "1000051", "2016-04-12",
                "Prescription Drugs", "GENERIC", "33.33"));

        assertPaid(result, "13.33", "20.00");
        assertEquals(money("6725.67"), result.individualAccumulated());
    }

    @Test
    void errorRowDoesNotTouchTheTotals() {
        // A12: an error between two claims; the second claim sees the same totals as if the error was not there
        AdjudicationResult before = run(claim("100003", "1000031", "2016-03-10",
                "Outpatient Services", "PRIMARY CARE OFFICE VISIT", "300.00"));
        AdjudicationResult error = run(claim("100003", "1000031", "2016-03-11",
                "Outpatient Services", "TEETH CLEANING", "300.00"));
        AdjudicationResult after = run(claim("100003", "1000031", "2016-03-12",
                "Outpatient Services", "PRIMARY CARE OFFICE VISIT", "100.00"));

        assertTrue(error.isError());
        assertEquals(ErrorCode.E0004, error.errorCode());
        assertNull(error.memberPays());
        assertNull(error.individualAccumulated());
        assertEquals(money("2800.00"), before.individualAccumulated());
        assertEquals(money("2900.00"), after.individualAccumulated());
    }

    @Test
    void unknownMemberIsE0001() {
        // Sample row 7
        AdjudicationResult result = run(claim("100001", "1000016", "2016-10-12",
                "Prescription Drugs", "GENERIC", "61.40"));

        assertEquals(ErrorCode.E0001, result.errorCode());
        assertEquals("Policy holder does not exist", result.errorMessage());
    }

    @Test
    void realTimeCallWithCallerTotals() {
        // A17: the REST API seeds a throwaway store with the caller's totals (samples/claim-row14.json)
        accumulators = new InMemoryAccumulatorStore(TestData.BENEFIT_YEAR,
                Map.of(1000012L, money("467.60")), Map.of(100001L, money("12000.00")));

        AdjudicationResult result = run(claim("100001", "1000012", "2016-08-09",
                "Prescription Drugs", "GENERIC", "240.60"));

        assertPaid(result, "96.24", "144.36");
        assertEquals(money("563.84"), result.individualAccumulated());
        assertEquals(money("12096.24"), result.familyAccumulated());
        assertEquals("ANNUAL DEDUCTIBLE (FAMILY) met, plan pays 60%", result.processingMessage());
    }

    @Test
    void unexpectedFailureBecomesE0099AndIsNotThrown() {
        MemberRepository broken = id -> {
            throw new IllegalStateException("Policy database is down");
        };
        ClaimValidator validator = new ClaimValidator(TestData.referenceData(), broken,
                TestData.PROCESSING_DATE, TestData.BENEFIT_YEAR);
        ClaimAdjudicator adjudicator = new ClaimAdjudicator(validator, new DeductibleService());

        AdjudicationResult result = adjudicator.adjudicate(claim("100001", "1000011", "2016-05-06",
                "Inpatient Hospital Care", "ROOM AND BOARD", "1000.00"), accumulators);

        assertEquals(ErrorCode.E0099, result.errorCode());
        assertEquals(money("0.00"), accumulators.individual(1000011));
    }

    @Test
    void needsAValidatorAndADeductibleService() {
        assertThrows(NullPointerException.class, () -> new ClaimAdjudicator(null, new DeductibleService()));
        assertThrows(NullPointerException.class, () -> new ClaimAdjudicator(TestData.validator(), null));
    }

    private AdjudicationResult run(Claim claim) {
        return adjudicator.adjudicate(claim, accumulators);
    }

    private static void assertPaid(AdjudicationResult result, String memberPays, String planPays) {
        assertFalse(result.isError(), () -> "unexpected " + result.errorCode());
        assertEquals(money(memberPays), result.memberPays(), "member pays");
        assertEquals(money(planPays), result.planPays(), "plan pays");
        assertEquals(Money.of(result.claim().billedAmount()), result.memberPays().add(result.planPays()),
                "member + plan = billed");
    }
}
