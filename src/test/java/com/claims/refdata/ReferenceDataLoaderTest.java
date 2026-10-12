package com.claims.refdata;

import com.claims.TestData;
import com.claims.domain.Member;
import com.claims.domain.Plan;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static com.claims.TestData.money;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reference data must load exactly, and bad reference data must stop the application at start-up. */
class ReferenceDataLoaderTest {

    private static final String PLANS = """
            planId,planName,coverageType,monthlyPremium,individualDeductible,familyDeductible
            P001,Bronze 6000/40%/HAS,Family,962.00,6000.00,12000.00
            """;
    private static final String COVERAGE = """
            mainCategory,subCategory,planId,rule
            Outpatient Services,X-RAYS,P001,40% AFTER DEDUCTIBLE
            Preventive Care,ROUTINE PHYSICAL EXAM,P001,No Charge
            """;
    private static final String POLICIES_HEADER =
            "policyId,policyHolderId,firstName,lastName,planId,coverageStartDate,coverageEndDate,"
                    + "individualAccumulated2016,familyAccumulated2016\n";

    @Test
    void loadsTheAssignmentWorkbookData() {
        ReferenceData data = TestData.referenceData();

        assertEquals(3, data.plans().size());
        assertEquals(10, data.members().size());

        Plan p002 = data.findPlan("P002").orElseThrow();
        assertEquals(money("5000.00"), p002.individualDeductible());
        assertEquals(money("10000.00"), p002.familyDeductible());
        assertEquals("$100 FLAT", p002.ruleFor("Emergency And Urgent Care", "URGENT CARE VISIT").orElseThrow().ruleText());
        assertEquals("No Charge", p002.ruleFor("Preventive Care", "MAMMOGRAMS").orElseThrow().ruleText());

        Member jacke = data.findMember(1000041).orElseThrow();
        assertEquals(100004, jacke.policyId());
        assertEquals("P003", jacke.planId());
        assertEquals(LocalDate.of(2016, 5, 31), jacke.coverageEnd());
        assertNull(data.findMember(1000011).orElseThrow().coverageEnd());
    }

    @Test
    void every84CoverageRowIsLoaded() {
        // 28 services x 3 plans in coverage.csv
        int rules = TestData.referenceData().plans().values().stream().mapToInt(p -> p.rules().size()).sum();

        assertEquals(84, rules);
    }

    @Test
    void openingAccumulatorsComeFromPolicyData() {
        ReferenceData data = TestData.referenceData();

        assertEquals(money("4000.00"), data.openingIndividual().get(1000021L));
        assertEquals(money("5560.42"), data.openingFamily().get(100004L));
        assertEquals(money("0.00"), data.openingFamily().get(100001L));
    }

    @Test
    void conflictingFamilyTotalsAreRejected() {
        // A15: the family total is repeated on every member row, so all copies must agree
        String policies = POLICIES_HEADER
                + "100001,1000011,Sam,Collins,P001,2004-01-01,,0.00,0.00\n"
                + "100001,1000012,Jina,Collins,P001,2008-12-05,,0.00,150.00\n";

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> ReferenceDataLoader.load(csv(PLANS), csv(COVERAGE), csv(policies)));
        assertTrue(e.getMessage().contains("policies.csv line 3"), e.getMessage());
        assertTrue(e.getMessage().contains("conflicts"), e.getMessage());
    }

    @Test
    void memberOnUnknownPlanIsRejected() {
        String policies = POLICIES_HEADER + "100001,1000011,Sam,Collins,P009,2004-01-01,,0.00,0.00\n";

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> ReferenceDataLoader.load(csv(PLANS), csv(COVERAGE), csv(policies)));
        assertTrue(e.getMessage().contains("unknown planId P009"), e.getMessage());
    }

    @Test
    void duplicatePolicyHolderIsRejected() {
        String policies = POLICIES_HEADER
                + "100001,1000011,Sam,Collins,P001,2004-01-01,,0.00,0.00\n"
                + "100001,1000011,Sam,Collins,P001,2004-01-01,,0.00,0.00\n";

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> ReferenceDataLoader.load(csv(PLANS), csv(COVERAGE), csv(policies)));
        assertTrue(e.getMessage().contains("duplicate policyHolderId 1000011"), e.getMessage());
    }

    @Test
    void planWithoutCoverageRulesIsRejected() {
        String plans = PLANS + "P002,Bronze 5000/50,Family,1017.00,5000.00,10000.00\n";
        String policies = POLICIES_HEADER + "100001,1000011,Sam,Collins,P001,2004-01-01,,0.00,0.00\n";

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> ReferenceDataLoader.load(csv(plans), csv(COVERAGE), csv(policies)));
        assertTrue(e.getMessage().contains("plan P002 has no coverage rules"), e.getMessage());
    }

    @Test
    void unknownRuleTextIsRejected() {
        String coverage = COVERAGE + "Outpatient Services,LAB TESTS,P001,80% BEFORE DEDUCTIBLE\n";

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> ReferenceDataLoader.load(csv(PLANS), csv(coverage), csv(POLICIES_HEADER)));
        assertTrue(e.getMessage().contains("coverage.csv line 4"), e.getMessage());
    }

    @Test
    void duplicateRuleIsRejectedEvenWithDifferentCase() {
        String coverage = COVERAGE + "outpatient services, x-rays ,P001,50% AFTER DEDUCTIBLE\n";

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> ReferenceDataLoader.load(csv(PLANS), csv(coverage), csv(POLICIES_HEADER)));
        assertTrue(e.getMessage().contains("duplicate rule"), e.getMessage());
    }

    @Test
    void rowWithTooFewColumnsIsRejected() {
        String policies = POLICIES_HEADER + "100001,1000011,Sam,Collins,P001\n";

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> ReferenceDataLoader.load(csv(PLANS), csv(COVERAGE), csv(policies)));
        assertTrue(e.getMessage().contains("expected 9 columns but found 5"), e.getMessage());
    }

    @Test
    void emptyFileIsRejected() {
        assertThrows(IllegalStateException.class,
                () -> ReferenceDataLoader.load(csv(""), csv(COVERAGE), csv(POLICIES_HEADER)));
    }

    @Test
    void blankLinesAreSkipped() {
        String policies = POLICIES_HEADER + "\n100001,1000011,Sam,Collins,P001,2004-01-01,,0.00,0.00\n\n";

        ReferenceData data = ReferenceDataLoader.load(csv(PLANS), csv(COVERAGE), csv(policies));

        assertEquals(1, data.members().size());
    }

    private static InputStream csv(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
