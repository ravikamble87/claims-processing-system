package com.claims.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * "$N FLAT": plan pays exactly N, member pays the rest, regardless of deductible status.
 * If the bill is smaller than N, the plan pays only the bill (assumption A7).
 */
public record FlatAmountRule(BigDecimal planAmount) implements CoverageRule {

    public FlatAmountRule {
        Objects.requireNonNull(planAmount, "planAmount");
        if (planAmount.signum() < 0) {
            throw new IllegalArgumentException("planAmount must not be negative: " + planAmount);
        }
        planAmount = Money.of(planAmount);
    }

    @Override
    public String ruleText() {
        return "$" + amountText() + " FLAT";
    }

    @Override
    public Split split(BigDecimal billed, DeductibleStatus status) {
        BigDecimal planPays = planAmount.min(billed);
        return new Split(billed.subtract(planPays), planPays,
                "Flat dollar coverage - plan pays $" + amountText() + " regardless of deductible");
    }

    private String amountText() {
        return planAmount.stripTrailingZeros().toPlainString();
    }
}
