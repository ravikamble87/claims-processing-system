package com.claims.cli;

import com.claims.batch.JobRunService;
import com.claims.batch.RunStats;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

/**
 * Batch mode (door B):  java -jar claims.jar input.csv output.csv
 * With no positional arguments the application keeps running as a web server (GUI + REST).
 * Exit code: 0 = completed, 1 = job failed, 2 = wrong arguments or unreadable files.
 */
@Component
public class BatchCommandLineRunner implements ApplicationRunner {

    private final JobRunService jobRunService;
    private final ConfigurableApplicationContext context;

    public BatchCommandLineRunner(JobRunService jobRunService, ConfigurableApplicationContext context) {
        this.jobRunService = jobRunService;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> files = args.getNonOptionArgs();
        if (files.isEmpty()) {
            return; // web mode
        }
        int exitCode;
        if (files.size() != 2) {
            System.err.println("Usage: java -jar claims.jar <inputTransactionFile.csv> <outputTransactionFile.csv>");
            exitCode = 2;
        } else {
            exitCode = runBatch(Path.of(files.get(0)), Path.of(files.get(1)));
        }
        int code = exitCode;
        System.exit(SpringApplication.exit(context, (ExitCodeGenerator) () -> code));
    }

    private int runBatch(Path input, Path output) {
        try {
            RunStats.Snapshot result = jobRunService.runAndWait(input, output);
            System.out.printf("%nClaims processed: %s%n", result.state());
            System.out.printf("  Input           : %s%n", result.inputFile());
            System.out.printf("  Output          : %s%n", result.outputFile());
            System.out.printf("  Rows            : %d (processed %d, errors %d)%n", result.rows(), result.processed(), result.errors());
            System.out.printf("  Billed / Plan / Member : %s / %s / %s%n", result.totalBilled(), result.planPaid(), result.memberPaid());
            if (!result.errorsByCode().isEmpty()) {
                System.out.printf("  Errors by code  : %s%n", result.errorsByCode());
            }
            if (result.state() != RunStats.State.COMPLETED) {
                System.out.printf("  Message         : %s%n", result.message());
                return 1;
            }
            return 0;
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return 2;
        }
    }
}
