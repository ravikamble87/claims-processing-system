package com.claims.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * One output row (SampleTransactionsProcessed). Either a processed claim or an error, never both.
 * Accumulator values are the totals AFTER this claim ("as of service date").
 */
public record AdjudicationResult(
        Claim claim,
        BigDecimal memberPays,
        BigDecimal planPays,
        String ruleUsed,
        BigDecimal individualAccumulated,
        BigDecimal familyAccumulated,
        ErrorCode errorCode,
        String processingMessage) {

    public AdjudicationResult {
        Objects.requireNonNull(claim, "claim");
    }

    public static AdjudicationResult processed(Claim claim, CoverageRule rule, Split split,
                                               BigDecimal individualAfter, BigDecimal familyAfter) {
        return new AdjudicationResult(claim, split.memberPays(), split.planPays(), rule.ruleText(),
                individualAfter, familyAfter, null, split.processingMessage());
    }

    /** Error rows keep their position and leave every payment and accumulator column empty (A12). */
    public static AdjudicationResult error(Claim claim, ErrorCode code) {
        return new AdjudicationResult(claim, null, null, null, null, null,
                Objects.requireNonNull(code, "code"), null);
    }

    public boolean isError() {
        return errorCode != null;
    }

    public String errorMessage() {
        return errorCode == null ? null : errorCode.message();
    }
}
