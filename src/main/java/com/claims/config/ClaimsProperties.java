package com.claims.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;

/**
 * Application settings under the "claims" prefix (application.yaml).
 *
 * @param processingDate "today" for the future-dated check; 2016-12-31 for the demo (assumption A11)
 * @param benefitYear    the only year we have accumulators for (assumption A10)
 * @param chunkSize      Spring Batch commit interval
 * @param refdata        locations of the reference CSV files (any Spring resource: classpath:, file:)
 */
@Validated
@ConfigurationProperties("claims")
public record ClaimsProperties(
        @NotNull LocalDate processingDate,
        @DefaultValue("2016") int benefitYear,
        @DefaultValue("1000") @Min(1) int chunkSize,
        @Valid @DefaultValue Refdata refdata) {

    public record Refdata(
            @DefaultValue("classpath:refdata/plans.csv") @NotBlank String plans,
            @DefaultValue("classpath:refdata/coverage.csv") @NotBlank String coverage,
            @DefaultValue("classpath:refdata/policies.csv") @NotBlank String policies) {
    }
}
