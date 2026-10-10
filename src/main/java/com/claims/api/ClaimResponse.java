package com.claims.api;

import com.claims.domain.AdjudicationResult;
import com.claims.domain.Claim;

import java.math.BigDecimal;

/** Same fields as one row of the processed output file. Accumulators are the totals after this claim. */
public record ClaimResponse(
        String policyId,
        String policyHolderId,
        String dateOfService,
        String mainCategory,
        String subCategory,
        String billedAmount,
        BigDecimal policyHolderPays,
        BigDecimal planPays,
        String ruleUsed,
        BigDecimal individualAccumulated,
        BigDecimal familyAccumulated,
        String errorCode,
        String errorMessage,
        String processingMessage) {

    static ClaimResponse from(AdjudicationResult result) {
        Claim claim = result.claim();
        return new ClaimResponse(claim.policyId(), claim.policyHolderId(), claim.dateOfService(),
                claim.mainCategory(), claim.subCategory(), claim.billedAmount(),
                result.memberPays(), result.planPays(), result.ruleUsed(),
                result.individualAccumulated(), result.familyAccumulated(),
                result.isError() ? result.errorCode().code() : null,
                result.errorMessage(), result.processingMessage());
    }
}
