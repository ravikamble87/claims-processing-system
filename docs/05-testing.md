# 05 · Testing

## Strategy

| Level | Tool | What |
| --- | --- | --- |
| Unit | JUnit 5 | Each rule, DeductibleService, ClaimValidator, every scenario + assumption |
| Regression | JUnit 5 | All 19 sample rows vs SampleTransactionsProcessed, column by column |
| API | MockMvc | REST endpoints, error responses |
| Batch | spring-batch-test | Full job: CSV in → CSV out → compare |

## Sample regression result

```text
TODO (you): paste test output, e.g. "19/19 rows match"
```

## Additional test cases

| # | Input | Expected | Actual | Proves |
| --- | --- | --- | --- | --- |
| T1 | Jacke Seegers 1000041, 2016-06-10 | E0003 | TODO | A9 |
| T2 | Jacke Seegers, 2016-05-31 | Processed | TODO | A9 inclusive end |
| T3 | San Mildred 1000061, 2016-01-02 | E0003 | TODO | A9 start |
| T4 | Tom Haskel 1000051 specialty $500 | Plan 350 / Member 150 | TODO | Already met |
| T5 | Sally Adams preventive $200 | Plan 200 / Member 0 | TODO | No Charge |
| T6 | Daniel Hayer urgent care $80 (P002) | Plan 80 / Member 0 | TODO | A7 |
| T7 | Daniel Hayer urgent care $250 (P002) | Plan 100 / Member 150 | TODO | Flat |
| T8 | Sam Collins under PolicyId 100002 | E0002 | TODO | A13 |
| T9 | Outpatient / TEETH CLEANING | E0004 | TODO | Invalid category |
| T10 | Mixed case + spaces category | Matched | TODO | A8 |
| T11 | Empty / "abc" / 0 amount | E0005 / E0006 / E0008 | TODO | Validation |
| T12 | P001 generic $33.33 after deductible | Plan 20.00 / Member 13.33 | TODO | A6 rounding |
| T13 | Error row mid-file | Position kept, totals unchanged | TODO | A12 |

## Coverage

TODO (you): JaCoCo % for `domain` and `service`.
