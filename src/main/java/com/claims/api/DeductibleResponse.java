package com.claims.api;

import java.math.BigDecimal;

public record DeductibleResponse(
        String planId,
        BigDecimal individualDeductible,
        BigDecimal familyDeductible,
        BigDecimal individualAccumulated,
        BigDecimal familyAccumulated,
        boolean individualMet,
        boolean familyMet,
        boolean met) {
}
