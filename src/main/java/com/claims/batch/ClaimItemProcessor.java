package com.claims.batch;

import com.claims.domain.AdjudicationResult;
import com.claims.domain.Claim;
import com.claims.service.AccumulatorStore;
import com.claims.service.ClaimAdjudicator;
import org.springframework.batch.infrastructure.item.ItemProcessor;

/**
 * Spring Batch adapter around ClaimAdjudicator. One instance per step execution (step-scoped bean),
 * holding that run's accumulator store. Never returns null, so no row is filtered out of the output.
 */
public class ClaimItemProcessor implements ItemProcessor<Claim, AdjudicationResult> {

    private final ClaimAdjudicator adjudicator;
    private final AccumulatorStore accumulators;

    public ClaimItemProcessor(ClaimAdjudicator adjudicator, AccumulatorStore accumulators) {
        this.adjudicator = adjudicator;
        this.accumulators = accumulators;
    }

    @Override
    public AdjudicationResult process(Claim claim) {
        return adjudicator.adjudicate(claim, accumulators);
    }
}
