package com.claims.config;

import com.claims.refdata.ReferenceData;
import com.claims.refdata.ReferenceDataLoader;
import com.claims.service.ClaimAdjudicator;
import com.claims.service.ClaimValidator;
import com.claims.service.DeductibleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;

/**
 * Wires the plain-Java core (domain + service) into Spring. The core itself has no Spring annotations (ADR 0005).
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ClaimsProperties.class)
public class ClaimsConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ClaimsConfiguration.class);

    @Bean
    ReferenceData referenceData(ClaimsProperties properties, ResourceLoader resourceLoader) throws IOException {
        ClaimsProperties.Refdata files = properties.refdata();
        try (InputStream plans = resourceLoader.getResource(files.plans()).getInputStream();
             InputStream coverage = resourceLoader.getResource(files.coverage()).getInputStream();
             InputStream policies = resourceLoader.getResource(files.policies()).getInputStream()) {
            ReferenceData data = ReferenceDataLoader.load(plans, coverage, policies);
            log.info("Loaded reference data: {} plans, {} members", data.plans().size(), data.members().size());
            return data;
        }
    }

    @Bean
    DeductibleService deductibleService() {
        return new DeductibleService();
    }

    @Bean
    ClaimValidator claimValidator(ReferenceData referenceData, ClaimsProperties properties) {
        return new ClaimValidator(referenceData, referenceData, properties.processingDate(), properties.benefitYear());
    }

    @Bean
    ClaimAdjudicator claimAdjudicator(ClaimValidator claimValidator, DeductibleService deductibleService) {
        return new ClaimAdjudicator(claimValidator, deductibleService);
    }
}
