package com.claims.refdata;

import com.claims.domain.CoverageKey;
import com.claims.domain.CoverageRule;
import com.claims.domain.Member;
import com.claims.domain.Money;
import com.claims.domain.Plan;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads plans.csv, coverage.csv and policies.csv (converted from the assignment workbook).
 * Fails fast at start-up on bad reference data: a wrong rule must never reach a claim.
 */
public final class ReferenceDataLoader {

    private ReferenceDataLoader() {
    }

    /**
     * Column layouts (header row is skipped):
     * <ul>
     *   <li>plans.csv: planId, planName, coverageType, monthlyPremium, individualDeductible, familyDeductible</li>
     *   <li>coverage.csv: mainCategory, subCategory, planId, rule</li>
     *   <li>policies.csv: policyId, policyHolderId, firstName, lastName, planId, coverageStartDate,
     *       coverageEndDate, individualAccumulated2016, familyAccumulated2016</li>
     * </ul>
     */
    public static ReferenceData load(InputStream plansCsv, InputStream coverageCsv, InputStream policiesCsv) {
        Map<String, Map<CoverageKey, CoverageRule>> rulesByPlan = loadCoverage(coverageCsv);
        Map<String, Plan> plans = loadPlans(plansCsv, rulesByPlan);

        Map<Long, Member> members = new LinkedHashMap<>();
        Map<Long, BigDecimal> openingIndividual = new HashMap<>();
        Map<Long, BigDecimal> openingFamily = new HashMap<>();
        for (Row row : rows(policiesCsv, "policies.csv", 9)) {
            long policyId = Long.parseLong(row.get(0));
            long policyHolderId = Long.parseLong(row.get(1));
            String planId = row.get(4);
            if (!plans.containsKey(planId)) {
                throw row.error("unknown planId " + planId);
            }
            Member member = new Member(policyHolderId, policyId, row.get(2), row.get(3), planId,
                    LocalDate.parse(row.get(5)), row.get(6).isBlank() ? null : LocalDate.parse(row.get(6)));
            if (members.put(policyHolderId, member) != null) {
                throw row.error("duplicate policyHolderId " + policyHolderId);
            }
            openingIndividual.put(policyHolderId, Money.of(row.get(7)));

            // A15: the family total is repeated on every member row; all copies must agree.
            BigDecimal family = Money.of(row.get(8));
            BigDecimal previous = openingFamily.putIfAbsent(policyId, family);
            if (previous != null && previous.compareTo(family) != 0) {
                throw row.error("family accumulator " + family + " conflicts with " + previous + " for policy " + policyId);
            }
        }
        return new ReferenceData(plans, members, openingIndividual, openingFamily);
    }

    private static Map<String, Map<CoverageKey, CoverageRule>> loadCoverage(InputStream coverageCsv) {
        Map<String, Map<CoverageKey, CoverageRule>> rulesByPlan = new HashMap<>();
        for (Row row : rows(coverageCsv, "coverage.csv", 4)) {
            CoverageKey key = new CoverageKey(row.get(0), row.get(1));
            CoverageRule rule;
            try {
                rule = CoverageRule.parse(row.get(3));
            } catch (IllegalArgumentException e) {
                throw row.error(e.getMessage());
            }
            if (rulesByPlan.computeIfAbsent(row.get(2), k -> new HashMap<>()).put(key, rule) != null) {
                throw row.error("duplicate rule for " + key + " on plan " + row.get(2));
            }
        }
        return rulesByPlan;
    }

    private static Map<String, Plan> loadPlans(InputStream plansCsv, Map<String, Map<CoverageKey, CoverageRule>> rulesByPlan) {
        Map<String, Plan> plans = new LinkedHashMap<>();
        for (Row row : rows(plansCsv, "plans.csv", 6)) {
            String planId = row.get(0);
            Map<CoverageKey, CoverageRule> rules = rulesByPlan.getOrDefault(planId, Map.of());
            if (rules.isEmpty()) {
                throw row.error("plan " + planId + " has no coverage rules");
            }
            plans.put(planId, new Plan(planId, row.get(1), row.get(2), Money.of(row.get(3)),
                    Money.of(row.get(4)), Money.of(row.get(5)), rules));
        }
        return plans;
    }

    private static List<Row> rows(InputStream in, String fileName, int expectedColumns) {
        List<Row> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            if (line == null) {
                throw new IllegalStateException(fileName + " is empty");
            }
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                List<String> fields = CsvLine.parse(line);
                Row row = new Row(fileName, lineNumber, fields);
                if (fields.size() < expectedColumns) {
                    throw row.error("expected " + expectedColumns + " columns but found " + fields.size());
                }
                rows.add(row);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + fileName, e);
        }
        return rows;
    }

    private record Row(String fileName, int lineNumber, List<String> fields) {

        String get(int index) {
            return fields.get(index).trim();
        }

        IllegalStateException error(String message) {
            return new IllegalStateException(fileName + " line " + lineNumber + ": " + message);
        }
    }
}
