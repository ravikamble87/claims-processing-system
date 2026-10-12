package com.claims.batch;

import com.claims.domain.AdjudicationResult;
import com.claims.domain.Claim;
import com.claims.domain.ErrorCode;
import com.claims.domain.PercentAfterDeductibleRule;
import com.claims.domain.Split;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.claims.TestData.claim;
import static com.claims.TestData.money;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** A23: same 14 columns and header as SampleTransactionsProcessed; input columns echoed as received. */
class ResultCsvFormatterTest {

    @Test
    void headerMatchesTheExpectedFile() throws IOException {
        List<String> expected = Files.readAllLines(Path.of("samples", "SampleTransactionsProcessed.csv"));

        assertEquals(expected.get(0), ResultCsvFormatter.HEADER);
    }

    @Test
    void processedRow() {
        Claim claim = claim("100001", "1000011", "2016-05-06", "Inpatient Hospital Care", "ROOM AND BOARD", "1000.00");
        Split split = new Split(money("1000.00"), money("0.00"),
                "ANNUAL DEDUCTIBLE (INDIVIDUAL or FAMILY) not met, plan pays 0%");
        AdjudicationResult result = AdjudicationResult.processed(claim,
                new PercentAfterDeductibleRule(new BigDecimal("40")), split, money("1000.00"), money("1000.00"));

        assertEquals("100001,1000011,2016-05-06,Inpatient Hospital Care,ROOM AND BOARD,1000.00,1000.00,0.00,"
                        + "40% AFTER DEDUCTIBLE,1000.00,1000.00,,,"
                        + "\"ANNUAL DEDUCTIBLE (INDIVIDUAL or FAMILY) not met, plan pays 0%\"",
                ResultCsvFormatter.format(result));
    }

    @Test
    void errorRowLeavesPaymentColumnsEmpty() {
        // Sample row 7
        Claim claim = claim("100001", "1000016", "2016-10-12", "Prescription Drugs", "GENERIC", "61.40");

        assertEquals("100001,1000016,2016-10-12,Prescription Drugs,GENERIC,61.40,,,,,,E0001,Policy holder does not exist,",
                ResultCsvFormatter.format(AdjudicationResult.error(claim, ErrorCode.E0001)));
    }

    @Test
    void rejectedValueIsEchoedAsReceived() {
        Claim claim = claim("100005", "1000051", "2016-06-05", "Outpatient Services", "X-RAYS", "abc");

        assertEquals("100005,1000051,2016-06-05,Outpatient Services,X-RAYS,abc,,,,,,E0006,Field is malformed,",
                ResultCsvFormatter.format(AdjudicationResult.error(claim, ErrorCode.E0006)));
    }

    @Test
    void valueWithCommaIsQuoted() {
        Claim claim = claim("100005", "1000051", "2016-06-08", "Outpatient Services", "MRI, CT, PET", "1200.00");

        assertEquals("100005,1000051,2016-06-08,Outpatient Services,\"MRI, CT, PET\",1200.00,,,,,,"
                        + "E0004,Invalid coverage category or sub category,",
                ResultCsvFormatter.format(AdjudicationResult.error(claim, ErrorCode.E0004)));
    }
}
