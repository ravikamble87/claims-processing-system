package com.claims;

import com.claims.batch.ClaimLineMapper;
import com.claims.batch.ResultCsvFormatter;
import com.claims.domain.AdjudicationResult;
import com.claims.service.AccumulatorStore;
import com.claims.service.ClaimAdjudicator;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Runs whole input files through the same pieces the batch job uses (line mapper → adjudicator → formatter),
 * in file order with one accumulator store, and compares every output line with the expected file.
 * <ul>
 *   <li>samples/: the 19 rows of SampleTransactions vs SampleTransactionsProcessed from the assignment</li>
 *   <li>testdata/: the same 19 rows followed by 61 of my own rows (TC01-TC61 in the workbook, orange/pink rows)</li>
 * </ul>
 */
class ExpectedOutputRegressionTest {

    @Test
    void sampleTransactionsMatchTheExpectedTab() throws IOException {
        List<String> input = Files.readAllLines(Path.of("samples", "SampleTransactions.csv"), StandardCharsets.UTF_8);
        List<String> expected = Files.readAllLines(Path.of("samples", "SampleTransactionsProcessed.csv"), StandardCharsets.UTF_8);

        assertSameOutput(expected, process(input));
    }

    @Test
    void customTransactionsMatchTheirExpectedResults() {
        List<String> input = readTestResource("/testdata/custom-transactions.csv");
        List<String> expected = readTestResource("/testdata/custom-expected.csv");

        assertEquals(81, input.size(), "header + 19 sample rows + 61 custom rows");
        assertSameOutput(expected, process(input));
    }

    private static List<String> process(List<String> inputLines) {
        ClaimAdjudicator adjudicator = TestData.adjudicator();
        AccumulatorStore accumulators = TestData.referenceData().newAccumulatorStore(TestData.BENEFIT_YEAR);
        ClaimLineMapper mapper = new ClaimLineMapper();

        List<String> output = new ArrayList<>();
        output.add(ResultCsvFormatter.HEADER);
        for (int i = 1; i < inputLines.size(); i++) {
            if (inputLines.get(i).isBlank()) {
                continue;
            }
            // +1 because the reader numbers the header as line 1
            AdjudicationResult result = adjudicator.adjudicate(mapper.mapLine(inputLines.get(i), i + 1), accumulators);
            output.add(ResultCsvFormatter.format(result));
        }
        return output;
    }

    private static void assertSameOutput(List<String> expected, List<String> actual) {
        assertEquals(expected.size(), actual.size(), "number of output lines");
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), actual.get(i), "output line " + (i + 1));
        }
    }

    private static List<String> readTestResource(String name) {
        try (InputStream in = ExpectedOutputRegressionTest.class.getResourceAsStream(name)) {
            if (in == null) {
                throw new IllegalStateException(name + " not found on the test classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
