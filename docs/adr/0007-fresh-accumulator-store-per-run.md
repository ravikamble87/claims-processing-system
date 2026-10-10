# ADR 0007: Each run gets a fresh accumulator store seeded from PolicyData

Status: Accepted · Date: 2026-10-10

## Context
Accumulators change with every processed claim. The demo has no Claims database; the opening year-to-date
values come from the PolicyData tab. The GUI can run the same file several times.

## Options considered
1. One shared, application-wide store — the second run of a file would start from the first run's totals
   and give different answers
2. A fresh in-memory store per run, seeded from PolicyData — every run is independent and repeatable
3. Accumulators in a database table — production design, but no source of truth to persist to in the demo

## Decision
Option 2. `ReferenceData.newAccumulatorStore()` creates the store; the step-scoped `ClaimItemProcessor` holds it
for one job execution. The REST API uses its own throwaway store seeded from the request (A17).

## Consequences
+ Same input file → same output file, every time (matches the expected tab on every run)
+ No shared mutable state between runs or threads
− Results are not persisted; production replaces the store with a table and a row lock per family
  (docs/04-scaling-and-deployment.md) behind the same `AccumulatorStore` port
