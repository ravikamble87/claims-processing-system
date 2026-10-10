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
| Tests | TODO (next commit) |
| Documentation | TODO |
| **Total** | TODO |

## Tools

| Tool | Used for |
| --- | --- |
| IntelliJ IDEA | Development, Git integration |
| Spring Initializr | Project skeleton (Boot 4.1.1, Java 21, Maven) |
| Git + GitHub (private repo) | Version control |
| Claude (AI assistant) | US health-insurance domain explanation; design review; CSV conversion of the workbook; code for the domain, service, reference-data, batch, CLI, API and GUI layers; documentation drafts |

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

## YYYY-MM-DD · h
- Did:
- Decided:
- Problem → fix:
- AI use:
