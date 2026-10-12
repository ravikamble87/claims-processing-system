package com.claims;

import com.claims.domain.Claim;
import com.claims.refdata.ReferenceData;
import com.claims.refdata.ReferenceDataLoader;
import com.claims.service.ClaimAdjudicator;
import com.claims.service.ClaimValidator;
import com.claims.service.DeductibleService;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Shared test fixtures. Uses the real reference data from src/main/resources/refdata, so the tests run
 * against the same plans and members as the demo. Same processing date and benefit year as application.yaml.
 */
public final class TestData {

    public static final LocalDate PROCESSING_DATE = LocalDate.of(2016, 12, 31);
    public static final int BENEFIT_YEAR = 2016;

    private static ReferenceData referenceData;

    private TestData() {
    }

    /** Loaded once; ReferenceData is immutable, so sharing it between tests is safe. */
    public static synchronized ReferenceData referenceData() {
        if (referenceData == null) {
            try (InputStream plans = refdata("plans.csv");
                 InputStream coverage = refdata("coverage.csv");
                 InputStream policies = refdata("policies.csv")) {
                referenceData = ReferenceDataLoader.load(plans, coverage, policies);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return referenceData;
    }

    public static ClaimValidator validator() {
        return new ClaimValidator(referenceData(), referenceData(), PROCESSING_DATE, BENEFIT_YEAR);
    }

    public static ClaimAdjudicator adjudicator() {
        return new ClaimAdjudicator(validator(), new DeductibleService());
    }

    public static Claim claim(String policyId, String policyHolderId, String dateOfService,
                              String mainCategory, String subCategory, String billedAmount) {
        return new Claim(1, policyId, policyHolderId, dateOfService, mainCategory, subCategory, billedAmount);
    }

    /** Expected amounts are written with 2 decimals, e.g. money("400.00"), because BigDecimal.equals checks scale. */
    public static BigDecimal money(String amount) {
        return new BigDecimal(amount);
    }

    private static InputStream refdata(String fileName) {
        InputStream in = TestData.class.getResourceAsStream("/refdata/" + fileName);
        if (in == null) {
            throw new IllegalStateException("refdata/" + fileName + " not found on the test classpath");
        }
        return in;
    }
}
