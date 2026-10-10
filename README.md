# Claims Processing System

Adjudicates health-insurance claims for ABC Health Insurance (fictitious) plans P001–P003:
for each claim it decides **how much the member pays and how much the plan pays**, using
individual and family annual deductibles and three coverage-rule types.

All 19 sample transactions reproduce `SampleTransactionsProcessed` exactly (byte-for-byte output file).

## Build and run

Requires **JDK 21**. The Maven wrapper downloads Maven itself.

```bash
./mvnw clean package          # Windows: mvnw.cmd clean package  → target/claims.jar
```

| Mode | How |
| --- | --- |
| A. GUI | `java -jar target/claims.jar` → open http://localhost:8080 |
| B. Batch (CLI) | `java -jar target/claims.jar samples/SampleTransactions.csv target/out.csv` |
| C. REST API | `curl -X POST http://localhost:8080/api/v1/claims/adjudicate -H "Content-Type: application/json" -d @samples/claim-row14.json` |
| Deductible service | `curl -X POST http://localhost:8080/api/v1/deductible/status -H "Content-Type: application/json" -d @samples/deductible-status.json` |

From the IDE: run `ClaimsProcessingSystemApplication` with no arguments for the GUI and API, or with
`samples/SampleTransactions.csv target/out.csv` as program arguments for batch mode.

CLI exit codes: `0` completed, `1` job failed, `2` wrong arguments or unreadable file.

### Endpoints

| Method and path | Purpose | Responses |
| --- | --- | --- |
| `POST /api/v1/claims/adjudicate` | One claim + current accumulators → split and new accumulators | 200 processed · 422 rejected by a rule (error code in body) · 400 malformed request · 500 unexpected (E0099) |
| `POST /api/v1/deductible/status` | Plan + accumulators → individual/family met? | 200 · 404 unknown plan · 400 |
| `POST /api/v1/jobs` | Start a batch run on server-side file paths (used by the GUI) | 202 `{runId}` · 400 bad path |
| `GET /api/v1/jobs/{runId}` | Progress and totals of a run | 200 · 404 |
| `GET /actuator/health` | Health check | 200 |
| `/h2-console` | Spring Batch job tables (JDBC URL `jdbc:h2:mem:claims`, user `sa`) | |

## The three decisions that matter most

1. **Deductible status is checked before each claim; a claim is never split** ([ADR 0002](docs/adr/0002-check-deductible-before-claim-no-split.md)).
2. **The member's share always adds to both accumulators**, even after the deductible is met ([assumption A3](docs/02-assumptions.md)).
3. **The percentage is the plan's share**, the opposite of usual US "coinsurance" wording ([ADR 0001](docs/adr/0001-plan-percent-is-plan-share.md)).

## Architecture at a glance

![Architecture](docs/diagrams/architecture.png)

One plain-Java core (`domain` + `service`, no Spring) is shared by all three modes. Spring Batch drives file
processing; the REST API calls the core directly. See [docs/03-design.md](docs/03-design.md).

## Project layout

```text
src/main/java/com/claims
  domain     business types and the three coverage rules (plain Java)
  service    ClaimValidator, DeductibleService, ClaimAdjudicator, accumulator store (plain Java)
  refdata    CSV loading of plans, coverage rules, policies
  config     Spring wiring + claims.* settings
  batch      Spring Batch job, reader/processor/writer, run tracking
  cli        command-line mode
  api        REST controllers
  web        job API for the GUI
src/main/resources
  refdata/   plans.csv, coverage.csv, policies.csv (from the assignment workbook)
  static/    index.html (GUI)
samples/     SampleTransactions.csv, SampleTransactionsProcessed.csv, request JSON examples
docs/        design, assumptions, ADRs, dev log
```

## Documentation

| Document | What it covers |
| --- | --- |
| [01 Problem and glossary](docs/01-problem-and-glossary.md) | Business terms, rules, worked scenarios |
| [02 Assumptions](docs/02-assumptions.md) | Every gap in the spec, the decision taken, and the test that proves it |
| [03 Design](docs/03-design.md) | Classes, packages, the three modes, flow of one claim |
| [04 Scaling and deployment](docs/04-scaling-and-deployment.md) | 700,000 claims/day, Spring Batch, Docker, Kubernetes |
| [05 Testing](docs/05-testing.md) | Test strategy and cases |
| [ADRs](docs/adr/) | One page per key decision |
| [Dev log](docs/dev-log.md) | Dated journal: work done, hours, tools (incl. AI), **issues and how they were resolved** |

## Tech stack

Java 21 · Spring Boot 4.1.1 · Spring Batch 6.0.5 (JDBC job repository) · Spring MVC · H2 (demo) · Maven wrapper
