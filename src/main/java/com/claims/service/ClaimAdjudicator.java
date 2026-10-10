package com.claims.service;

import com.claims.domain.AdjudicationResult;
import com.claims.domain.Claim;
import com.claims.domain.DeductibleStatus;
import com.claims.domain.ErrorCode;
import com.claims.domain.Split;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * The single rulebook used by every door (GUI, CLI, REST):
 * validate → find rule → deductible status BEFORE the claim → split → add member share to both accumulators.
 * Always returns a result; an error is a result, never an exception (assumption A12).
 */
public final class ClaimAdjudicator {

    private static final Logger log = LoggerFactory.getLogger(ClaimAdjudicator.class);

    private final ClaimValidator validator;
    private final DeductibleService deductibleService;

    public ClaimAdjudicator(ClaimValidator validator, DeductibleService deductibleService) {
        this.validator = Objects.requireNonNull(validator, "validator");
        this.deductibleService = Objects.requireNonNull(deductibleService, "deductibleService");
    }

    public AdjudicationResult adjudicate(Claim claim, AccumulatorStore accumulators) {
        try {
            return switch (validator.validate(claim)) {
                case ValidationOutcome.Invalid invalid -> AdjudicationResult.error(claim, invalid.errorCode());
                case ValidationOutcome.Valid valid -> process(valid, accumulators);
            };
        } catch (RuntimeException e) {
            // Log position and code only: claim content is health data (PHI) and stays out of logs.
            log.error("Unexpected error adjudicating claim at line {}", claim.lineNo(), e);
            return AdjudicationResult.error(claim, ErrorCode.E0099);
        }
    }

    private AdjudicationResult process(ValidationOutcome.Valid claim, AccumulatorStore accumulators) {
        long memberId = claim.policyHolderId();
        long policyId = claim.policyId();

        // A1: status is decided from the totals BEFORE this claim; the claim is never split.
        DeductibleStatus status = deductibleService.status(
                claim.plan(), accumulators.individual(memberId), accumulators.family(policyId));

        Split split = claim.rule().split(claim.billedAmount(), status);

        // A3: the member's share always counts, even after the deductible is met.
        accumulators.add(memberId, policyId, split.memberPays());

        BigDecimal individualAfter = accumulators.individual(memberId);
        BigDecimal familyAfter = accumulators.family(policyId);
        return AdjudicationResult.processed(claim.claim(), claim.rule(), split, individualAfter, familyAfter);
    }
}
