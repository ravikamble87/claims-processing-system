package com.claims.refdata;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal RFC 4180 helpers shared by the reference-data loader, the batch reader and the result writer,
 * so quoting behaves the same everywhere. Handles quoted fields with commas and doubled quotes ("").
 */
public final class CsvLine {

    private CsvLine() {
    }

    public static List<String> parse(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else if (c == '"') {
                    inQuotes = false;
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else if (c != '\r') {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields;
    }

    /** Quotes a value only when it needs it (comma, quote or line break). null becomes an empty field. */
    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.indexOf(',') >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }

    public static String join(List<String> values) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                line.append(',');
            }
            line.append(escape(values.get(i)));
        }
        return line.toString();
    }
}
