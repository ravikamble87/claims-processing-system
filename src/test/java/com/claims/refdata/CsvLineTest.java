package com.claims.refdata;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvLineTest {

    @Test
    void splitsPlainFields() {
        assertEquals(List.of("100001", "1000011", "2016-05-06", "Inpatient Hospital Care", "ROOM AND BOARD", "1000.00"),
                CsvLine.parse("100001,1000011,2016-05-06,Inpatient Hospital Care,ROOM AND BOARD,1000.00"));
    }

    @Test
    void keepsCommasInsideQuotes() {
        assertEquals(List.of("Outpatient Services", "MRI, CT, PET", "P001"),
                CsvLine.parse("Outpatient Services,\"MRI, CT, PET\",P001"));
    }

    @Test
    void doubledQuoteIsOneQuote() {
        assertEquals(List.of("say \"hi\"", "x"), CsvLine.parse("\"say \"\"hi\"\"\",x"));
    }

    @Test
    void keepsEmptyFieldsIncludingTheLastOne() {
        assertEquals(List.of("100001", "", "x", ""), CsvLine.parse("100001,,x,"));
    }

    @Test
    void dropsWindowsLineEnding() {
        assertEquals(List.of("a", "b"), CsvLine.parse("a,b\r"));
    }

    @Test
    void escapesOnlyWhenNeeded() {
        assertEquals("", CsvLine.escape(null));
        assertEquals("GENERIC", CsvLine.escape("GENERIC"));
        assertEquals("\"ANNUAL DEDUCTIBLE (INDIVIDUAL) met, plan pays 40%\"",
                CsvLine.escape("ANNUAL DEDUCTIBLE (INDIVIDUAL) met, plan pays 40%"));
        assertEquals("\"say \"\"hi\"\"\"", CsvLine.escape("say \"hi\""));
    }

    @Test
    void joinThenParseGivesTheSameValues() {
        List<String> values = Arrays.asList("100001", "MRI, CT, PET", "say \"hi\"", "", "40% AFTER DEDUCTIBLE");

        assertEquals(values, CsvLine.parse(CsvLine.join(values)));
    }

    @Test
    void joinWritesNullAsEmpty() {
        assertEquals("a,,b", CsvLine.join(Arrays.asList("a", null, "b")));
    }
}
