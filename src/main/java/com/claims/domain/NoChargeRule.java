package com.claims.domain;

import java.math.BigDecimal;

/** "No Charge": plan pays the whole bill, member pays 0, regardless of deductible status. */
public record NoChargeRule() implements CoverageRule {

    @Override
    public String ruleText() {
        return "No Charge";
    }

    @Override
    public Split split(BigDecimal billed, DeductibleStatus status) {
        return new Split(Money.ZERO, billed, "Preventive Care - No Charge to policyholder");
    }
}
