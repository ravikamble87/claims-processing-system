# 02 · Assumptions

The assignment states it contains intentional ambiguities. Each one below is resolved explicitly.
"Tested by" links the assumption to the test that proves it.

## Proved by the expected output

| # | Assumption | Why the application needs it | Evidence | Tested by |
| --- | --- | --- | --- | --- |
| A1 | Deductible status is evaluated BEFORE the claim; a claim is never split at the threshold | One deterministic answer per claim | Row 3 | TODO |
| A2 | "Met" means accumulated >= limit | Exact comparison operator | Row 4 | TODO |
| A3 | Member's share always adds to both accumulators, even after met | Output columns J/K; true YTD total | Rows 4, 8–10 | TODO |
| A4 | The percentage is the plan's share | Opposite of usual US coinsurance wording | Row 4 | TODO |
| A5 | Family = PolicyId | Key for the family accumulator | Rows 5–14 | TODO |

## Our own decisions

| # | Assumption | Why | Tested by |
| --- | --- | --- | --- |
| A6 | BigDecimal, scale 2, HALF_UP on plan share; member pays billed − plan | No float errors; shares always sum to the bill | TODO |
| A7 | Flat amount is capped at the billed amount | Plan never pays more than the bill | TODO |
| A8 | Category match on main + sub, case/space-insensitive; grouped rows expanded in reference data | LAB TESTS / X-RAYS exist under two main categories | TODO |
| A9 | Coverage active when start <= date <= end; empty end = active | Expired-coverage rule | TODO |
| A10 | Accumulators reset per calendar year; only 2016 supported | Data is "this year (2016)" | TODO |
| A11 | Processing date is configurable (`claims.processing-date`, 2016-12-31 for demo) | Future-dated check in a pretend 2016 | TODO |
| A12 | Error rows keep position, empty amounts, change no accumulator | Assignment + row 7 | TODO |
| A13 | Unknown member → E0001; member under another policy → E0002 | Protects family accumulators | TODO |
| A14 | Both limits met → message says INDIVIDUAL | Deterministic message | TODO |
| A15 | Family accumulator stored once per PolicyId; conflicting rows fail load | Sheet repeats it per member | TODO |
| A16 | Rule text: `40% AFTER DEDUCTIBLE`, `No Charge`, `$120 FLAT` | Matches expected output | TODO |
| A17 | REST API is stateless: caller sends totals, gets new totals back | Assignment wording; reusable service | TODO |
| A18 | Input dates accepted as `2016-05-06` or `5/6/2016` | Excel exports | TODO |
| A19 | No claim ID, so no duplicate detection | Cannot tell repeat visit from copy | — |
| A20 | Premium, copay, OOP max, networks not modelled | Not in the rules | — |

## Added while implementing

| # | Assumption | Why | Tested by |
| --- | --- | --- | --- |
| A21 | REST returns 200 for a processed claim, 422 for a claim rejected by a business rule (body carries the error code), 400 for a malformed request | Lets callers tell "your request is broken" from "the claim is not payable" | TODO |
| A22 | GUI file paths are paths on the machine running the application | The assignment asks for input and output paths, and the demo runs locally; a hosted version would upload files instead | TODO |
| A23 | Output file: same 14 columns and header as SampleTransactionsProcessed; input columns echoed as received; amounts with 2 decimals; UTF-8, LF line endings | Output compares byte-for-byte with the expected file on any OS | TODO |
| A24 | Every batch run starts from the PolicyData opening accumulators; runs do not affect each other; GUI runs queue one at a time | Re-running the same file gives the same answer (ADR 0007) | TODO |
| A25 | Ids must be positive whole numbers; amounts may have at most 2 decimals | "250.505" or "-5" in an id are data errors (E0006), not values to round | TODO |

TODO (you): add every new assumption you make while coding, the day you make it.

## Error codes

| Code | Message | When |
| --- | --- | --- |
| E0001 | Policy holder does not exist | Given by assignment |
| E0002 | Policy holder does not belong to this policy | Member under another PolicyId |
| E0003 | Coverage not active on date of service | Before start / after end |
| E0004 | Invalid coverage category or sub category | No rule for main + sub |
| E0005 | Required field is missing | Empty input column |
| E0006 | Field is malformed | Bad date / amount |
| E0007 | Date of service is in the future | After processing date |
| E0008 | Billed amount must be greater than zero | Zero or negative |
| E0009 | Date of service outside benefit year | Not 2016 |
| E0010 | Plan not configured | Plan missing |
| E0099 | Unexpected processing error | Safety net |

Validation order (first failure wins): E0005, E0006, E0008, E0007, E0009, E0001, E0002, E0003, E0010, E0004.
