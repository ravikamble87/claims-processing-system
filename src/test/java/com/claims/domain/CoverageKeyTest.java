package com.claims.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** A8: match on main + sub category, ignoring case and extra spaces. */
class CoverageKeyTest {

    @Test
    void ignoresCaseAndExtraSpaces() {
        assertEquals(new CoverageKey("Inpatient Hospital Care", "SURGERY"),
                new CoverageKey("  inpatient   hospital care ", " surgery  "));
    }

    @Test
    void sameSubCategoryUnderDifferentMainCategoryIsADifferentKey() {
        // LAB TESTS is 80% outpatient but 40% inpatient on P001, so the sub category alone is not enough.
        assertNotEquals(new CoverageKey("Outpatient Services", "LAB TESTS"),
                new CoverageKey("Inpatient Hospital Care", "LAB TESTS"));
    }

    @Test
    void storesTheNormalisedValues() {
        CoverageKey key = new CoverageKey(" Prescription  Drugs ", "generic");

        assertEquals("PRESCRIPTION DRUGS", key.mainCategory());
        assertEquals("GENERIC", key.subCategory());
    }

    @Test
    void rejectsNull() {
        assertThrows(NullPointerException.class, () -> new CoverageKey(null, "GENERIC"));
        assertThrows(NullPointerException.class, () -> new CoverageKey("Prescription Drugs", null));
    }
}
