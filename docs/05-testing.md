# 05 · Testing

## Strategy

| Level | Tool | What | Status |
| --- | --- | --- | --- |
| Unit | JUnit 5 | Each rule, DeductibleService, ClaimValidator, every scenario + assumption | Done – 20 classes, 191 cases |
| Regression | JUnit 5 | All 19 sample rows vs SampleTransactionsProcessed, line by line; plus 61 custom rows | Done – `ExpectedOutputRegressionTest` |
| API | MockMvc | REST endpoints, error responses | Not yet |
| Batch | spring-batch-test | Full job: CSV in → CSV out → compare | Not yet (the regression test covers the same mapper → adjudicator → formatter chain without Spring) |

## How to run

```text
mvnw test                                   all tests
mvnw test -Dtest=ExpectedOutputRegressionTest   only the file comparisons
```

Tests run from the project root, so `samples/` is found as a relative path. Reference data comes from `src/main/resources/refdata/`, custom test files from `src/test/resources/testdata/`.

## Test classes

| Package | Class | What it proves |
| --- | --- | --- |
| `com.claims` | `ExpectedOutputRegressionTest` | 19/19 sample rows and 80/80 sample + custom rows match the expected files exactly |
| `com.claims` | `TestData` | Shared fixtures: real reference data, `PROCESSING_DATE` 2016-12-31, `BENEFIT_YEAR` 2016, `money("0.00")` helper |
| `domain` | `MoneyTest` | Scale 2, HALF_UP, `percentOf` rounding (A6) |
| `domain` | `CoverageRuleTest` | Parsing `40% AFTER DEDUCTIBLE`, `No Charge`, `$120 FLAT` / `120`; unknown text rejected |
| `domain` | `PercentAfterDeductibleRuleTest` | 0% until met, plan % after (A4); messages; individual wins when both met (A14); shares add up to the bill |
| `domain` | `FlatAmountRuleTest` | Flat regardless of deductible; capped at the bill (A7) |
| `domain` | `NoChargeRuleTest` | Plan pays all, member 0 |
| `domain` | `CoverageKeyTest` | Case/space-insensitive match on main + sub (A8) |
| `domain` | `MemberTest` | Start and end dates both covered; open-ended coverage (A9) |
| `domain` | `DeductibleStatusTest`, `AccumulatorTest`, `AdjudicationResultTest` | Met/basis combinations; immutable accumulator; error rows carry only error columns (A12) |
| `service` | `DeductibleServiceTest` | `>=` limit for all 3 plans (A2); scale does not matter |
| `service` | `ClaimValidatorTest` | Every error code E0001–E0010 and the validation order (first failure wins) |
| `service` | `ClaimAdjudicatorTest` | No split when crossing (A1); share keeps counting (A3); family met only; error row leaves totals unchanged; REST example; E0099 |
| `service` | `InMemoryAccumulatorStoreTest` | Family = PolicyId (A5); independent stores per run (A24) |
| `refdata` | `ReferenceDataLoaderTest`, `CsvLineTest` | Real data loads (3 plans, 10 members, 84 rules); bad data stops start-up (A15) |
| `batch` | `ClaimLineMapperTest`, `ResultCsvFormatterTest` | Raw text in, trimmed; header and output format identical to the expected file (A23) |

## Sample regression result

```text
ExpectedOutputRegressionTest
  sampleTransactionsMatchTheExpectedTab          19/19 rows match
  customTransactionsMatchTheirExpectedResults    80/80 rows match (19 sample + 61 custom)
All tests: 191 passed, 0 failed (javac 21 run, see dev-log issue 21)

TODO (you): replace with the mvnw test summary from my machine
```

## Additional test cases

The full list (TC01–TC61) is in the workbook: orange/pink rows 21–81 of `SampleTransactions` / `SampleTransactionsProcessed`, and the `CustomTestCases` tab. The same rows are in `src/test/resources/testdata/`.

| # | Input | Expected | Actual | Proves | Test |
| --- | --- | --- | --- | --- | --- |
| T1 | Jacke Seegers 1000041, 2016-06-10 | E0003 | Pass | A9 | `ClaimValidatorTest`, TC37 |
| T2 | Jacke Seegers, 2016-05-31 | Processed | Pass | A9 inclusive end | `ClaimValidatorTest.boundaryDatesAreAccepted`, TC24 |
| T3 | San Mildred 1000061, 2016-01-02 | E0003 | Pass | A9 start | `ClaimValidatorTest`, TC38 |
| T4 | Tom Haskel 1000051 specialty $500 | Plan 350 / Member 150 | Pass | Already met | `ClaimAdjudicatorTest`, TC08 |
| T5 | Sally Adams preventive $200 | Plan 200 / Member 0 | Pass | No Charge | `NoChargeRuleTest`, TC09 |
| T6 | Daniel Hayer urgent care $80 (P002) | Plan 80 / Member 0 | Pass | A7 | `ClaimAdjudicatorTest.flatAmountIsCappedAtTheBill`, TC12 |
| T7 | Daniel Hayer urgent care $250 (P002) | Plan 100 / Member 150 | Pass | Flat | `ClaimAdjudicatorTest`, TC11 |
| T8 | Sam Collins under PolicyId 100002 | E0002 | Pass | A13 | `ClaimValidatorTest`, TC36 |
| T9 | Outpatient / TEETH CLEANING | E0004 | Pass | Invalid category | `ClaimValidatorTest`, TC41 |
| T10 | Mixed case + spaces category | Matched | Pass | A8 | `ClaimValidatorTest.matchesCategoriesIgnoringCaseAndSpaces`, TC29 |
| T11 | Empty / "abc" / 0 amount | E0005 / E0006 / E0008 | Pass | Validation | `ClaimValidatorTest`, TC44, TC47, TC51 |
| T12 | P001 generic $33.33 after deductible | Plan 20.00 / Member 13.33 | Pass | A6 rounding | `MoneyTest`, `ClaimAdjudicatorTest`, TC30 |
| T13 | Error row mid-file | Position kept, totals unchanged | Pass | A12 | `ClaimAdjudicatorTest.errorRowDoesNotTouchTheTotals`, TC34–TC58 |
| T14 | Exactly $6,000.00 then another claim | Second claim covered | Pass | A2 | `ClaimAdjudicatorTest.exactlyAtTheDeductibleCountsAsMet`, TC04–TC05 |
| T15 | Claim crossing the deductible | Not split, member pays all | Pass | A1 | `ClaimAdjudicatorTest.claimThatCrossesTheDeductibleIsNotSplit`, TC01–TC03 |
| T16 | Family met, individual not (Jack Collins) | Plan 70% | Pass | Family rule | `ClaimAdjudicatorTest.familyMetButIndividualNot`, TC06 |
| T17 | Inpatient vs outpatient LAB TESTS (P001) | 40% vs 80% | Pass | A8 main + sub | `ClaimAdjudicatorTest`, TC16–TC17 |
| T18 | "MRI, CT, PET" as one sub-category | E0004 | Pass | A8 (see dev-log issue 17) | `ClaimValidatorTest`, TC59 |
| T19 | Two errors in one row | First in order wins | Pass | Validation order | `ClaimValidatorTest.firstFailureWins`, TC55–TC57 |
| T20 | Future date / year 2015 | E0007 / E0009 | Pass | A10, A11 | `ClaimValidatorTest`, TC53–TC54 |

## Problems found while testing

See `dev-log.md`, issues 15–22: running totals across custom rows, independent expected values, the TC59 expectation, `BigDecimal` scale, reaching E0010/E0099, Excel vs CSV values, running without Maven, and proving the tests can fail.

## Coverage

TODO (you): JaCoCo % for `domain` and `service` (plugin not added yet).
