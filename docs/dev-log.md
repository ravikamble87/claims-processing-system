# Dev log

A dated record of the work: what was done, time spent, tools used (including AI), the problems hit and how each was resolved.

## Summary

| Phase | Hours |
| --- | --- |
| Understanding the problem and design | TODO |
| Domain + services | TODO |
| Spring Batch + CLI | TODO |
| REST API | TODO |
| GUI | TODO |
| Docker | TODO |
| Tests | TODO |
| Documentation | TODO |
| **Total** | TODO |

## Tools

| Tool | Used for |
| --- | --- |
| IntelliJ IDEA | Development, Git integration |
| Spring Initializr | Project skeleton (Boot 4.1.1, Java 21, Maven) |
| Git + GitHub (private repo) | Version control |
| Claude (AI assistant) | US health-insurance domain explanation; design review; CSV conversion of the workbook; code for the domain, service, reference-data, batch, CLI, API and GUI layers; unit tests and extra test transactions; documentation drafts |

**What AI produced vs what I did:** TODO (be specific, for example: "Claude generated the code in commit X; I reviewed every class, changed …, and can explain …").

---

## Issues and resolutions

| # | Symptom | Root cause | Resolution | Evidence |
| --- | --- | --- | --- | --- |
| 1 | Spring Initializr listed both **Spring Batch** and **Spring Batch JDBC** | Spring Boot 4 / Spring Batch 6 split the starter: the plain one keeps job metadata in memory, the JDBC one in database tables | Chose **Spring Batch JDBC** (restartable jobs, progress readable from the job repository, same setup on Oracle later) | ADR 0004; `spring-boot-starter-batch-jdbc` in `pom.xml` |
| 2 | IntelliJ **New → Java Class** showed Class, Interface, Enum, Annotation, Exception, but **no Record** | Project JDK was set to **Java 27**, an early-access build. The IDE did not recognise 27 as a language level and fell back to an older one, which hides Record (needs Java 16+) | Switched the project SDK and language level to **JDK 21** (Microsoft OpenJDK 21.0.11, already installed). Workaround while fixing: create a Class and type `public record …` | Record option visible after Maven reload |
| 3 | `pom.xml` still had `<java.version>27</java.version>` after the IDE change | IntelliJ re-reads the Java level from the pom on every Maven reload, so IDE-only changes do not stick; `javac` 21 cannot compile `--release 27` | Set `<java.version>21</java.version>` in the pom | `pom.xml` |
| 4 | The Excel workbook could not be read row-by-row | Note rows inside the data tabs; a line break inside "Bronze 6000/40%/HAS"; one coverage cell grouping 7 inpatient sub-services; flat amounts stored as bare numbers (100, 120); a stray header row | Converted to 5 CSVs: `plans.csv`, `coverage.csv` (84 rows, one per sub-service per plan), `policies.csv`, plus the two sample files. Rule text normalised to `40% AFTER DEDUCTIBLE`, `No Charge`, `$120 FLAT` | `src/main/resources/refdata/`, `samples/` |
| 5 | First CSV export had Windows line endings (CRLF) | The converter's CSV writer defaults to `\r\n` | Regenerated with LF; `.gitattributes` now forces `*.csv eol=lf` so expected and actual files compare byte-for-byte on any OS | `.gitattributes` |
| 6 | `mvnw` committed as **non-executable** (`100644`) | Windows has no executable bit, so Git recorded the wrapper as a plain file; `./mvnw` would fail with *Permission denied* in Docker or CI | `git update-index --chmod=+x mvnw` | `git ls-files -s mvnw` shows `100755` |
| 7 | `.gitattributes` only covered `mvnw` and `*.cmd` | Initializr default | Extended to shell scripts, Dockerfile, YAML, CSV (LF) and binaries | `.gitattributes` |
| 8 | Reference CSVs were not in the first commit | Files were prepared outside the project | Added under `src/main/resources/refdata/` (loaded at start-up) and `samples/` (demo input + expected output) | this commit |
| 9 | Spring Batch code from older tutorials did not match the library | **Spring Batch 6 moved packages and APIs**: `org.springframework.batch.item` → `org.springframework.batch.infrastructure.item`; `Job`/`Step` → `…core.job.Job` / `…core.step.Step`; `JobLauncher` deprecated → `JobOperator.start(job, params)`; `chunk(size, txManager)` deprecated → `chunk(size).transactionManager(tm)` | Wrote the batch layer against the Spring Batch 6.0.5 source code (the version Boot 4.1.1 uses) | `ClaimJobConfig`, `JobRunService` |
| 10 | Re-running the same input file would fail with `JobInstanceAlreadyCompleteException` | Spring Batch treats identical identifying parameters as the same, already finished job instance | Every run adds a unique `runId` job parameter | `JobRunService` |
| 11 | Output file would have CRLF on Windows and LF on Linux | `FlatFileItemWriter` defaults to the OS line separator | `.lineSeparator("\n")` on the writer | `ClaimJobConfig.resultWriter` |
| 12 | Output row numbers would be off by one | `FlatFileItemReader` counts the skipped header as line 1 | `lineNo = lineNumber - 1` in `ClaimLineMapper` | sample output matches |
| 13 | CLI mode would also start Tomcat on port 8080 | Spring MVC on the classpath makes Boot start a web server by default | `main()` switches to `WebApplicationType.NONE` when file arguments are passed | `ClaimsProcessingSystemApplication` |
| 14 | Could not run `mvn` in the AI sandbox | Maven Central was blocked by the sandbox network policy | Compiled every class against the Spring Boot 4.1.1, Spring Batch 6.0.5 and Spring Framework 7.0.9 sources from GitHub; planted 5 deliberate API mistakes to prove the type check was real (all 5 caught); ran the 19 sample rows through the real loader, validator, adjudicator, line mapper and CSV formatter: output **identical byte-for-byte** to `SampleTransactionsProcessed.csv` | Still to do on my machine: `mvnw clean verify` and a run of all three modes |
| 15 | Needed test transactions beyond the 19 samples, but every custom row changes the running totals for the rows after it | Batch mode carries accumulators across the whole file (A17, A24), so an expected value depends on file position, not only on the row | Appended 61 custom rows (TC01–TC61) **after** the 19 sample rows, in the same tab, coloured orange (processed) / pink (error), with the test id in a cell comment. Expected values were calculated in file order. Unit tests do not depend on order: `@BeforeEach` gives every test a fresh store from PolicyData | Workbook rows 21–81; `src/test/resources/testdata/custom-*.csv`; `CustomTestCases` tab |
| 16 | Risk of "testing the code with itself": generating expected results by running my own program would pass even if the rules were wrong | Expected output must come from somewhere independent of the implementation | Calculated expected values with a separate, small model of the rules (outside the Java code) and first proved that model reproduces all 19 rows of `SampleTransactionsProcessed` exactly. Only then compared the Java output with it | `ExpectedOutputRegressionTest`: 19/19 sample rows and 80/80 sample + custom rows match line by line |
| 17 | Custom row TC59 `Outpatient Services / "MRI, CT, PET"` was first expected to be **processed**, but the program returned **E0004** | The workbook shows "MRI, CT, PET" as one cell, but `coverage.csv` has one row per service (A8). A real claim is for one service, so the full label is not a valid sub-category | Kept the program's behaviour (E0004) and corrected the expected result. Added TC60 (`PET` alone, processed) so both sides of the rule are tested | `ClaimValidatorTest` "full grouped label"; workbook TC59/TC60 |
| 18 | Amount assertions would fail even when the numbers are equal (`400.0` vs `400.00`) | `BigDecimal.equals` compares scale as well as value | All expected amounts written with 2 decimals through `TestData.money("400.00")`, matching `Money.SCALE`. Added a test proving `DeductibleService` uses `compareTo`, so `6000` and `6000.00` both count as met | `TestData`; `DeductibleServiceTest.scaleDoesNotMatter` |
| 19 | E0010 (plan not configured) and E0099 (unexpected error) could not be produced from the CSV data | The loader refuses a member on an unknown plan at start-up (fail fast), and nothing in normal data throws | Used the ports: a `MemberRepository` lambda returning a member on plan `P999` for E0010, and one that throws for E0099. No mocking library needed | `ClaimValidatorTest.memberWhosePlanIsNotConfigured`; `ClaimAdjudicatorTest.unexpectedFailureBecomesE0099AndIsNotThrown` |
| 20 | Excel and CSV disagree on how values look (dates are real dates in Excel, text in CSV; leading spaces) | Excel stores typed values; the batch reader trims every field (`ClaimLineMapper`) and echoes the trimmed value | Test CSVs keep the raw text, including `9/3/2016`, `abc`, `10.555` and padded categories; expected output echoes the trimmed value, exactly like the real writer | TC29, TC47–TC50, TC61 |
| 21 | Could not run `mvnw test` in the AI sandbox | Maven Central blocked again; the Linux VM on my machine only has Java 11 | Compiled `domain`, `service`, `refdata` and the two batch helpers with `javac 21` against small stand-ins for JUnit, SLF4J and Spring's `LineMapper`, and ran all tests with a tiny runner: **191 passed, 0 failed** | Still to do on my machine: `mvnw test` in IntelliJ, paste the result into `05-testing.md` |
| 22 | How do I know the tests can actually fail? | A test suite that never fails proves nothing | Planted 6 bugs, one at a time, in a scratch copy: `>` instead of `>=` for "met" (5 tests failed), stop adding the member share after "met" (7), flat amount not capped (3), rounding DOWN instead of HALF_UP (6), wrong basis in the message (7), coverage end date exclusive (5). Every bug was caught | Kept the list here; re-run any time by editing one line and running the tests |

TODO (you): add a row every time something breaks, the same day.

---

## 2026-10-09 · TODO h
- Read the assignment; mapped business terms; identified ambiguities (A1–A20)
- Converted the workbook to CSV reference data (issue 4)
- Created the Spring Boot project (Web MVC, Spring Batch JDBC, H2, Validation, Actuator) (issue 1)
- Fixed the JDK: 27 early-access → 21 (issues 2, 3)
- Generated the domain model; verified it against all 19 sample rows
- Created the private GitHub repo and pushed the initial commit

## 2026-10-10 · TODO h
- Added the service layer: `DeductibleService`, `ClaimValidator`, `ClaimAdjudicator`, accumulator store
- Added reference-data loading from CSV, failing fast on bad data (A15)
- Added the Spring Batch job (door B, CLI) and the background runner + progress API used by the GUI (door A)
- Added the REST API (door C) and the reusable deductible-status endpoint
- Fixed line endings and the `mvnw` executable bit (issues 5–7)
- Adapted to Spring Batch 6 API changes (issues 9–13)
- Verified compile and sample output (issue 14)
- Decided: fresh accumulator store per run (ADR 0007); one executable, mode chosen by arguments (ADR 0008)

## 2026-10-12 · TODO h
- Added 61 extra test transactions to the workbook (TC01–TC61), in the same tabs, colour-coded, plus a `CustomTestCases` tab listing every case and the class it tests (issues 15, 20)
- Added unit tests: 20 test classes, 191 test cases across `domain`, `service`, `refdata`, `batch`, plus a file-level regression test for the 19 sample rows and the 80 sample + custom rows
- Problem → fix: expected values from an independent model, not from my own code (issue 16); TC59 expectation corrected to E0004 (issue 17); `BigDecimal` scale in assertions (issue 18); E0010/E0099 reached through the repository ports (issue 19)
- Verified: 191/191 pass with `javac 21`; 6 planted bugs all caught (issues 21, 22)
- AI use: Claude wrote the test classes and test data; I reviewed them against the assumptions (A1–A25) and can explain each test

## YYYY-MM-DD · h
- Did:
- Decided:
- Problem → fix:
- AI use:
