package com.claims.domain;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** One row of PlanDescriptions plus that plan's column of PlanCoverage. */
public record Plan(
        String planId,
        String planName,
        String coverageType,
        BigDecimal monthlyPremium,          // informational only, never used in claim math (A20)
        BigDecimal individualDeductible,
        BigDecimal familyDeductible,
        Map<CoverageKey, CoverageRule> rules) {

    public Plan {
        Objects.requireNonNull(planId, "planId");
        Objects.requireNonNull(individualDeductible, "individualDeductible");
        Objects.requireNonNull(familyDeductible, "familyDeductible");
        rules = Map.copyOf(Objects.requireNonNull(rules, "rules"));   // immutable, safe to share across threads
    }

    public Optional<CoverageRule> ruleFor(String mainCategory, String subCategory) {
        return Optional.ofNullable(rules.get(new CoverageKey(mainCategory, subCategory)));
    }
}
