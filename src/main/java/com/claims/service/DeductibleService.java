package com.claims.service;

import com.claims.domain.DeductibleStatus;
import com.claims.domain.Plan;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * The reusable "accumulated deductible" capability the assignment asks for.
 * Called before each claim (assumption A1); "met" means accumulated >= limit (assumption A2).
 * Exposed over REST by DeductibleController so other systems can use it.
 */
public final class DeductibleService {

    public DeductibleStatus status(Plan plan, BigDecimal individualAccumulated, BigDecimal familyAccumulated) {
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(individualAccumulated, "individualAccumulated");
        Objects.requireNonNull(familyAccumulated, "familyAccumulated");
        return new DeductibleStatus(
                individualAccumulated.compareTo(plan.individualDeductible()) >= 0,
                familyAccumulated.compareTo(plan.familyDeductible()) >= 0);
    }
}
