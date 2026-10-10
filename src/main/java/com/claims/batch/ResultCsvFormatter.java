package com.claims.batch;

import com.claims.domain.AdjudicationResult;
import com.claims.domain.Claim;
import com.claims.refdata.CsvLine;

import java.math.BigDecimal;
import java.util.Arrays;

/**
 * Formats one output row in the column order of the SampleTransactionsProcessed tab.
 * Input columns are echoed exactly as received, so an error row shows the value that was rejected.
 * Plain Java (no Spring) so the same format is used by the batch writer and can be unit-tested directly.
 */
public final class ResultCsvFormatter {

    public static final String HEADER = String.join(",",
            "PolicyId",
            "Policy holder Id",
            "Date of service",
            "Coverage Main Category",
            "Coverage Sub Category",
            "Billed Amount",
            "Policy Holder pays",
            "Plan Pays",
            "Rule used",
            "Individual accumulated deductible as of service date",
            "Family accumulated deductible as of service date",
            "Error Code",
            "Error Message",
            "Processing message");

    private ResultCsvFormatter() {
    }

    public static String format(AdjudicationResult result) {
        Claim claim = result.claim();
        return CsvLine.join(Arrays.asList(
                claim.policyId(),
                claim.policyHolderId(),
                claim.dateOfService(),
                claim.mainCategory(),
                claim.subCategory(),
                claim.billedAmount(),
                amount(result.memberPays()),
                amount(result.planPays()),
                result.ruleUsed(),
                amount(result.individualAccumulated()),
                amount(result.familyAccumulated()),
                result.isError() ? result.errorCode().code() : null,
                result.errorMessage(),
                result.processingMessage()));
    }

    private static String amount(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }
}
