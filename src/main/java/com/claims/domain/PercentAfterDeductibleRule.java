package com.claims.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * "X% AFTER DEDUCTIBLE": member pays 100% until the deductible is met, then the PLAN pays X% (assumption A4).
 * Plan share is rounded to cents; member pays the remainder so the two always add up to the bill (A6).
 */
public record PercentAfterDeductibleRule(BigDecimal planPercent) implements CoverageRule {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public PercentAfterDeductibleRule {
        Objects.requireNonNull(planPercent, "planPercent");
        if (planPercent.signum() < 0 || planPercent.compareTo(HUNDRED) > 0) {
            throw new IllegalArgumentException("planPercent must be 0..100: " + planPercent);
        }
    }

    @Override
    public String ruleText() {
        return percentText() + "% AFTER DEDUCTIBLE";
    }

    @Override
    public Split split(BigDecimal billed, DeductibleStatus status) {
        if (!status.isMet()) {
            return new Split(billed, Money.ZERO,
                    "ANNUAL DEDUCTIBLE (INDIVIDUAL or FAMILY) not met, plan pays 0%");
        }
        BigDecimal planPays = Money.percentOf(billed, planPercent);
        return new Split(billed.subtract(planPays), planPays,
                "ANNUAL DEDUCTIBLE (" + status.basis() + ") met, plan pays " + percentText() + "%");
    }

    private String percentText() {
        return planPercent.stripTrailingZeros().toPlainString();
    }
}
