package com.claims.domain;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One coverage rule from the PlanCoverage tab. Exactly three kinds exist, so the interface is sealed:
 * the compiler then forces every switch overrule types to handle all three.
 */
public sealed interface CoverageRule
        permits PercentAfterDeductibleRule, NoChargeRule, FlatAmountRule {

    /** Text for the "Rule used" output column, e.g. "40% AFTER DEDUCTIBLE", "No Charge", "$120 FLAT". */
    String ruleText();

    /** Divides the billed amount. status = deductible status BEFORE this claim. */
    Split split(BigDecimal billed, DeductibleStatus status);

    Pattern PERCENT = Pattern.compile("^(\\d+(?:\\.\\d+)?)%\\s+AFTER\\s+DEDUCTIBLE$");
    Pattern FLAT = Pattern.compile("^\\$?(\\d+(?:\\.\\d+)?)(?:\\s+FLAT)?$");

    /** Builds a rule from coverage.csv text: "40% AFTER DEDUCTIBLE", "No Charge", "$120 FLAT" (or "120"). */
    static CoverageRule parse(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Coverage rule text is empty");
        }
        String t = text.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
        if (t.equals("NO CHARGE")) {
            return new NoChargeRule();
        }
        Matcher percent = PERCENT.matcher(t);
        if (percent.matches()) {
            return new PercentAfterDeductibleRule(new BigDecimal(percent.group(1)));
        }
        Matcher flat = FLAT.matcher(t);
        if (flat.matches()) {
            return new FlatAmountRule(Money.of(flat.group(1)));
        }
        throw new IllegalArgumentException("Unknown coverage rule: " + text);
    }
}
