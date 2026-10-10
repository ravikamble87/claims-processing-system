package com.claims.batch;

import com.claims.domain.AdjudicationResult;
import com.claims.domain.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

/**
 * Live progress and totals of one job run, read by the GUI and printed by the CLI.
 * Updated by the batch thread after each chunk, read by web threads, hence synchronized.
 */
public final class RunStats {

    public enum State { QUEUED, RUNNING, COMPLETED, FAILED }

    private final String runId;
    private final String inputFile;
    private final String outputFile;
    private final Instant queuedAt = Instant.now();
    private Instant finishedAt;
    private State state = State.QUEUED;
    private String message;
    private long rows;
    private long processed;
    private BigDecimal totalBilled = Money.ZERO;
    private BigDecimal planPaid = Money.ZERO;
    private BigDecimal memberPaid = Money.ZERO;
    private final Map<String, Long> errorsByCode = new TreeMap<>();

    public RunStats(String runId, String inputFile, String outputFile) {
        this.runId = runId;
        this.inputFile = inputFile;
        this.outputFile = outputFile;
    }

    public synchronized void running() {
        state = State.RUNNING;
    }

    public synchronized void record(AdjudicationResult result) {
        rows++;
        if (result.isError()) {
            errorsByCode.merge(result.errorCode().code(), 1L, Long::sum);
            return;
        }
        processed++;
        planPaid = planPaid.add(result.planPays());
        memberPaid = memberPaid.add(result.memberPays());
        totalBilled = totalBilled.add(result.planPays()).add(result.memberPays());
    }

    public synchronized void finished(boolean success, String message) {
        this.state = success ? State.COMPLETED : State.FAILED;
        this.message = message;
        this.finishedAt = Instant.now();
    }

    public synchronized Snapshot snapshot() {
        long errors = errorsByCode.values().stream().mapToLong(Long::longValue).sum();
        return new Snapshot(runId, state, inputFile, outputFile, queuedAt, finishedAt, rows, processed, errors,
                totalBilled, planPaid, memberPaid, Map.copyOf(errorsByCode), message);
    }

    public String runId() {
        return runId;
    }

    /** Immutable view returned as JSON by the job API. */
    public record Snapshot(String runId, State state, String inputFile, String outputFile,
                           Instant queuedAt, Instant finishedAt,
                           long rows, long processed, long errors,
                           BigDecimal totalBilled, BigDecimal planPaid, BigDecimal memberPaid,
                           Map<String, Long> errorsByCode, String message) {
    }
}
