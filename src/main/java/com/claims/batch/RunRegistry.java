package com.claims.batch;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Keeps the stats of runs started since the application started (demo scope; not persisted). */
@Component
public class RunRegistry {

    private final Map<String, RunStats> runs = new ConcurrentHashMap<>();

    public RunStats register(RunStats stats) {
        runs.put(stats.runId(), stats);
        return stats;
    }

    public Optional<RunStats> find(String runId) {
        return Optional.ofNullable(runs.get(runId));
    }

    public List<RunStats.Snapshot> recent() {
        return runs.values().stream()
                .map(RunStats::snapshot)
                .sorted(Comparator.comparing(RunStats.Snapshot::queuedAt).reversed())
                .toList();
    }
}
