package com.claims.service;

import com.claims.TestData;
import com.claims.domain.DeductibleStatus;
import com.claims.domain.Plan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static com.claims.TestData.money;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** A2: "met" means accumulated >= limit. Limits: P001 6000/12000, P002 5000/10000, P003 4000/8000. */
class DeductibleServiceTest {

    private final DeductibleService service = new DeductibleService();

    @ParameterizedTest(name = "{0} individual {1} / family {2}")
    @CsvSource({
            "P001, 0.00,    0.00,     false, false",
            "P001, 5999.99, 5999.99,  false, false",
            "P001, 6000.00, 6000.00,  true,  false",   // exactly at the limit counts (sample row 4)
            "P001, 0.00,    11999.99, false, false",
            "P001, 0.00,    12000.00, false, true",    // Jina's case, sample row 14
            "P001, 6600.00, 12000.00, true,  true",
            "P002, 4000.00, 4000.00,  false, false",
            "P002, 5200.00, 5200.00,  true,  false",   // Daniel, sample row 17
            "P003, 4460.82, 4460.82,  true,  false",   // Mack, already over 4000
            "P003, 3999.99, 8000.00,  false, true"
    })
    void comparesAccumulatedWithThePlanLimits(String planId, BigDecimal individual, BigDecimal family,
                                              boolean individualMet, boolean familyMet) {
        Plan plan = TestData.referenceData().findPlan(planId).orElseThrow();

        DeductibleStatus status = service.status(plan, individual, family);

        assertEquals(new DeductibleStatus(individualMet, familyMet), status);
    }

    @Test
    void scaleDoesNotMatter() {
        // 6000 and 6000.00 are the same amount; compareTo, not equals, decides.
        Plan p001 = TestData.referenceData().findPlan("P001").orElseThrow();

        assertEquals(new DeductibleStatus(true, false), service.status(p001, new BigDecimal("6000"), money("0.00")));
    }

    @Test
    void rejectsMissingInput() {
        Plan p001 = TestData.referenceData().findPlan("P001").orElseThrow();

        assertThrows(NullPointerException.class, () -> service.status(null, money("0.00"), money("0.00")));
        assertThrows(NullPointerException.class, () -> service.status(p001, null, money("0.00")));
        assertThrows(NullPointerException.class, () -> service.status(p001, money("0.00"), null));
    }
}
