# 04 · Scaling and deployment

## Sizing

| Measure | Value |
| --- | --- |
| Average rate | ~8 claims/s (700,000 ÷ 86,400) |
| Nightly 2-hour window (assumed) | ~97 claims/s |
| Commits at chunk size 1,000 | 700 per night |

**The constraint is correctness, not throughput:** a family's accumulator must never be updated by two workers at once.

## Design for production volume

- **Partition by PolicyId** (`hash mod N`): one family always goes to one worker, in file order.
- **Staging table with `seq`** → export `ORDER BY seq` keeps original row order.
- **One transaction per chunk** covers results + accumulator updates → safe restart.
- **Batch vs real-time on the same family:** `SELECT … FOR UPDATE` on the family accumulator row.
- **Accumulator ledger** (append-only) for audit and reversals.

## Phases

| Phase | What | Done when |
| --- | --- | --- |
| 1 | Spring Boot jar, H2, single-threaded batch | All three doors demo on laptop |
| 2 | Docker multi-stage image + docker-compose (app + DB) | `docker compose up` runs the demo |
| 3 | Kubernetes (only if uptime requires): API Deployment + HPA, batch CronJob | — |

Public cloud (AWS): EKS, RDS (Oracle / Aurora PostgreSQL), S3 for files, Secrets Manager, OpenTelemetry + Grafana.

## Non-functional

| Area | Approach |
| --- | --- |
| Security | PHI (HIPAA): TLS, OAuth2/JWT on API, no names/amounts in logs |
| Reliability | Restartable job, idempotent re-run |
| Observability | Micrometer metrics, structured logs with correlation ID |
| Extensibility | New rule = new `CoverageRule` class; plans/rules are data |

TODO (you): record what you actually built vs what stays a design proposal.
