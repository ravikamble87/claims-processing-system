package com.claims.batch;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Starts the claim job for the CLI (synchronously) and for the GUI (in the background).
 * Uses JobOperator: in Spring Batch 6, JobLauncher is deprecated in favour of JobOperator.start(job, params).
 */
@Service
public class JobRunService {

    private static final Logger log = LoggerFactory.getLogger(JobRunService.class);

    private final JobOperator jobOperator;
    private final Job claimJob;
    private final RunRegistry registry;
    /** One background run at a time; further GUI requests queue in order. */
    private final ExecutorService background = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "claim-job-runner");
        t.setDaemon(true);
        return t;
    });

    public JobRunService(JobOperator jobOperator, Job claimJob, RunRegistry registry) {
        this.jobOperator = jobOperator;
        this.claimJob = claimJob;
        this.registry = registry;
    }

    /** CLI: runs the job and returns when it has finished. */
    public RunStats.Snapshot runAndWait(Path inputFile, Path outputFile) {
        RunStats stats = prepare(inputFile, outputFile);
        execute(stats, inputFile, outputFile);
        return stats.snapshot();
    }

    /** GUI: queues the job and returns its run id immediately; progress is read via the RunRegistry. */
    public String start(Path inputFile, Path outputFile) {
        RunStats stats = prepare(inputFile, outputFile);
        background.execute(() -> execute(stats, inputFile, outputFile));
        return stats.runId();
    }

    private RunStats prepare(Path inputFile, Path outputFile) {
        Path in = inputFile.toAbsolutePath().normalize();
        Path out = outputFile.toAbsolutePath().normalize();
        if (!Files.isRegularFile(in) || !Files.isReadable(in)) {
            throw new IllegalArgumentException("Input file not found or not readable: " + in);
        }
        if (in.equals(out)) {
            throw new IllegalArgumentException("Output file must be different from the input file");
        }
        Path parent = out.getParent();
        if (parent == null || !Files.isDirectory(parent)) {
            throw new IllegalArgumentException("Output folder does not exist: " + parent);
        }
        return registry.register(new RunStats(UUID.randomUUID().toString(), in.toString(), out.toString()));
    }

    private void execute(RunStats stats, Path inputFile, Path outputFile) {
        stats.running();
        // runId makes every run a new job instance, so the same file can be processed again.
        JobParameters parameters = new JobParametersBuilder()
                .addString(ClaimJobConfig.PARAM_INPUT, inputFile.toAbsolutePath().normalize().toString())
                .addString(ClaimJobConfig.PARAM_OUTPUT, outputFile.toAbsolutePath().normalize().toString())
                .addString(ClaimJobConfig.PARAM_RUN_ID, stats.runId())
                .toJobParameters();
        try {
            JobExecution execution = jobOperator.start(claimJob, parameters);
            boolean success = execution.getStatus() == BatchStatus.COMPLETED;
            String message = success ? "Completed" : "Job ended with status " + execution.getStatus()
                    + execution.getAllFailureExceptions().stream().findFirst()
                    .map(e -> ": " + e.getMessage()).orElse("");
            stats.finished(success, message);
        } catch (Exception e) {
            log.error("Claim job {} could not run", stats.runId(), e);
            stats.finished(false, e.getMessage());
        }
    }

    @PreDestroy
    void shutdown() {
        background.shutdown();
    }
}
