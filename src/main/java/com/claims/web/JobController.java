package com.claims.web;

import com.claims.batch.JobRunService;
import com.claims.batch.RunRegistry;
import com.claims.batch.RunStats;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.util.List;

/**
 * Backend for the GUI (door A): start a batch run on a file path, then poll its progress.
 * Paths are on the machine running the app (local demo; assumption A22).
 */
@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobRunService jobRunService;
    private final RunRegistry registry;

    public JobController(JobRunService jobRunService, RunRegistry registry) {
        this.jobRunService = jobRunService;
        this.registry = registry;
    }

    public record StartJobRequest(@NotBlank String inputFile, @NotBlank String outputFile) {
    }

    public record StartJobResponse(String runId) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public StartJobResponse start(@Valid @RequestBody StartJobRequest request) {
        String runId = jobRunService.start(Path.of(request.inputFile().trim()), Path.of(request.outputFile().trim()));
        return new StartJobResponse(runId);
    }

    @GetMapping("/{runId}")
    public RunStats.Snapshot status(@PathVariable String runId) {
        return registry.find(runId)
                .map(RunStats::snapshot)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown run " + runId));
    }

    @GetMapping
    public List<RunStats.Snapshot> recent() {
        return registry.recent();
    }
}
