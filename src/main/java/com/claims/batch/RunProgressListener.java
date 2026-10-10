package com.claims.batch;

import com.claims.domain.AdjudicationResult;
import org.springframework.batch.core.listener.ItemWriteListener;
import org.springframework.batch.infrastructure.item.Chunk;

/** After each chunk is written, adds its rows to the run's live stats (GUI progress, CLI summary). */
public class RunProgressListener implements ItemWriteListener<AdjudicationResult> {

    private final RunStats stats;

    public RunProgressListener(RunStats stats) {
        this.stats = stats;
    }

    @Override
    public void afterWrite(Chunk<? extends AdjudicationResult> items) {
        for (AdjudicationResult result : items) {
            stats.record(result);
        }
    }
}
