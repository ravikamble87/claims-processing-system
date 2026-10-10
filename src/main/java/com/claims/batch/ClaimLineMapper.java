package com.claims.batch;

import com.claims.domain.Claim;
import com.claims.refdata.CsvLine;
import org.springframework.batch.infrastructure.item.file.LineMapper;

import java.util.List;

/**
 * Turns one CSV line into a Claim of RAW strings. Never rejects a line: a missing column becomes "",
 * which ClaimValidator reports as E0005, so the row keeps its place in the output (assumption A12).
 */
public class ClaimLineMapper implements LineMapper<Claim> {

    @Override
    public Claim mapLine(String line, int lineNumber) {
        List<String> f = CsvLine.parse(line);
        // FlatFileItemReader counts the header as line 1, so the first claim is line 2 -> claim #1.
        return new Claim(lineNumber - 1L, field(f, 0), field(f, 1), field(f, 2), field(f, 3), field(f, 4), field(f, 5));
    }

    private static String field(List<String> fields, int index) {
        return index < fields.size() ? fields.get(index).trim() : "";
    }
}
