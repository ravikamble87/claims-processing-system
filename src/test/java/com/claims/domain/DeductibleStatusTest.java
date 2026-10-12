package com.claims.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeductibleStatusTest {

    @ParameterizedTest
    @CsvSource({
            "false, false, false, INDIVIDUAL or FAMILY",
            "true,  false, true,  INDIVIDUAL",
            "false, true,  true,  FAMILY",
            "true,  true,  true,  INDIVIDUAL"     // A14: individual wins when both are met
    })
    void metAndBasis(boolean individualMet, boolean familyMet, boolean expectedMet, String expectedBasis) {
        DeductibleStatus status = new DeductibleStatus(individualMet, familyMet);

        assertEquals(expectedMet, status.isMet());
        assertEquals(expectedBasis, status.basis());
    }
}
