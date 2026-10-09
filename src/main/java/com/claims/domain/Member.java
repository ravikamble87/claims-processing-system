package com.claims.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * One row of PolicyData: a policy holder (US industry term: member).
 * policyId identifies the family (assumption A5).
 */
public record Member(
        long policyHolderId,
        long policyId,
        String firstName,
        String lastName,
        String planId,
        LocalDate coverageStart,
        LocalDate coverageEnd) {          // null = coverage still active

    public Member {
        Objects.requireNonNull(planId, "planId");
        Objects.requireNonNull(coverageStart, "coverageStart");
        if (coverageEnd != null && coverageEnd.isBefore(coverageStart)) {
            throw new IllegalArgumentException("coverageEnd before coverageStart for " + policyHolderId);
        }
    }

    /** start <= date <= end, both ends inclusive; empty end means active (assumption A9). */
    public boolean isCoveredOn(LocalDate date) {
        return !date.isBefore(coverageStart) && (coverageEnd == null || !date.isAfter(coverageEnd));
    }
}
