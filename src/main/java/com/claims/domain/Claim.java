package com.claims.domain;

/**
 * One input row from SampleTransactions, kept as RAW strings.
 * Parsing happens in ClaimValidator, so a bad value becomes an error code (E0005 / E0006)
 * instead of an exception that would drop the row from the output.
 */
public record Claim(
        long lineNo,                 // position in the input file; output keeps this order
        String policyId,
        String policyHolderId,
        String dateOfService,        // 2016-05-06 or 5/6/2016 (assumption A18)
        String mainCategory,
        String subCategory,
        String billedAmount) {
}
