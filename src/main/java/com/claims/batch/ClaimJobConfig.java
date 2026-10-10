package com.claims.batch;

import com.claims.config.ClaimsProperties;
import com.claims.domain.AdjudicationResult;
import com.claims.domain.Claim;
import com.claims.refdata.ReferenceData;
import com.claims.service.ClaimAdjudicator;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.FlatFileItemWriter;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * One job, one chunk-oriented step: read CSV line → adjudicate → write CSV line.
 * Single-threaded on purpose: claims are processed in file order and each family's accumulator is updated
 * by one thread only (ADR 0006). Spring Batch 6 package names (org.springframework.batch.infrastructure.item…).
 */
@Configuration(proxyBeanMethods = false)
public class ClaimJobConfig {

    public static final String JOB_NAME = "claimJob";
    public static final String PARAM_INPUT = "inputFile";
    public static final String PARAM_OUTPUT = "outputFile";
    public static final String PARAM_RUN_ID = "runId";

    @Bean
    Job claimJob(JobRepository jobRepository, Step adjudicateClaimsStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(adjudicateClaimsStep)
                .build();
    }

    @Bean
    Step adjudicateClaimsStep(JobRepository jobRepository,
                              PlatformTransactionManager transactionManager,
                              ClaimsProperties properties,
                              FlatFileItemReader<Claim> claimReader,
                              ClaimItemProcessor claimItemProcessor,
                              FlatFileItemWriter<AdjudicationResult> resultWriter,
                              RunProgressListener runProgressListener) {
        return new StepBuilder("adjudicateClaims", jobRepository)
                .<Claim, AdjudicationResult>chunk(properties.chunkSize())
                .transactionManager(transactionManager)
                .reader(claimReader)
                .processor(claimItemProcessor)
                .writer(resultWriter)
                .listener(runProgressListener)
                .build();
    }

    @Bean
    @StepScope
    FlatFileItemReader<Claim> claimReader(@Value("#{jobParameters['" + PARAM_INPUT + "']}") String inputFile) {
        return new FlatFileItemReaderBuilder<Claim>()
                .name("claimReader")
                .resource(new FileSystemResource(inputFile))
                .encoding("UTF-8")
                .linesToSkip(1)
                .lineMapper(new ClaimLineMapper())
                .build();
    }

    @Bean
    @StepScope
    FlatFileItemWriter<AdjudicationResult> resultWriter(@Value("#{jobParameters['" + PARAM_OUTPUT + "']}") String outputFile) {
        return new FlatFileItemWriterBuilder<AdjudicationResult>()
                .name("resultWriter")
                .resource(new FileSystemResource(outputFile))
                .encoding("UTF-8")
                .lineSeparator("\n")
                .shouldDeleteIfExists(true)
                .headerCallback(writer -> writer.write(ResultCsvFormatter.HEADER))
                .lineAggregator(ResultCsvFormatter::format)
                .build();
    }

    /** A fresh accumulator store per run, seeded from PolicyData (assumption A24). */
    @Bean
    @StepScope
    ClaimItemProcessor claimItemProcessor(ClaimAdjudicator adjudicator, ReferenceData referenceData,
                                          ClaimsProperties properties) {
        return new ClaimItemProcessor(adjudicator, referenceData.newAccumulatorStore(properties.benefitYear()));
    }

    @Bean
    @StepScope
    RunProgressListener runProgressListener(@Value("#{jobParameters['" + PARAM_RUN_ID + "']}") String runId,
                                            @Value("#{jobParameters['" + PARAM_INPUT + "']}") String inputFile,
                                            @Value("#{jobParameters['" + PARAM_OUTPUT + "']}") String outputFile,
                                            RunRegistry registry) {
        RunStats stats = registry.find(runId)
                .orElseGet(() -> registry.register(new RunStats(runId, inputFile, outputFile)));
        return new RunProgressListener(stats);
    }
}
