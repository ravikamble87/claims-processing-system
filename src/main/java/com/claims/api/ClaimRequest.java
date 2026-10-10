package com.claims.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * One claim for real-time adjudication. Claim fields are strings, exactly like a CSV row, so the same
 * ClaimValidator produces the same error codes in every mode. The caller supplies the current accumulators
 * (in production these would be looked up from the database; assumption A17).
 */
public record ClaimRequest(
        String policyId,
        String policyHolderId,
        String dateOfService,
        String mainCategory,
        String subCategory,
        String billedAmount,
        @NotNull @DecimalMin("0.00") BigDecimal individualAccumulated,
        @NotNull @DecimalMin("0.00") BigDecimal familyAccumulated) {
}
