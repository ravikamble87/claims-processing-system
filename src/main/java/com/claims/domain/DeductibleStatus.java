package com.claims.domain;

/**
 * Answer from DeductibleService: was the deductible met BEFORE this claim? (assumption A1)
 * Met means accumulated >= limit (assumption A2).
 */
public record DeductibleStatus(boolean individualMet, boolean familyMet) {

    public boolean isMet() {
        return individualMet || familyMet;
    }

    /**
     * Which limit to name in the processing message.
     * Individual wins when both are met (assumption A14).
     */
    public String basis() {
        if (individualMet) {
            return "INDIVIDUAL";
        }
        if (familyMet) {
            return "FAMILY";
        }
        return "INDIVIDUAL or FAMILY";
    }
}
