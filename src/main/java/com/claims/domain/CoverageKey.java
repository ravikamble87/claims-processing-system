package com.claims.domain;

import java.util.Locale;
import java.util.Objects;

/**
 * Lookup key for a coverage rule: main category + sub category (assumption A8).
 * Normalised so "inpatient hospital care" / " surgery " matches "Inpatient Hospital Care" / "SURGERY".
 * Both parts are needed: LAB TESTS and X-RAYS exist under Outpatient AND Inpatient with different rates.
 */
public record CoverageKey(String mainCategory, String subCategory) {

    public CoverageKey {
        mainCategory = normalise(mainCategory, "mainCategory");
        subCategory = normalise(subCategory, "subCategory");
    }

    private static String normalise(String value, String name) {
        Objects.requireNonNull(value, name);
        return value.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }
}
