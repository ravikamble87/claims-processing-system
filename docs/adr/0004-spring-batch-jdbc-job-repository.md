# ADR 0004: Spring Batch with a JDBC job repository

Status: Accepted · Date: 2026-10-09

## Context
Spring Boot 4 offers "Spring Batch" (in-memory job repository) and "Spring Batch JDBC" (database-backed).

## Decision
Spring Batch JDBC, with H2 for the demo and Oracle in production.

## Consequences
+ Restart from the last committed chunk
+ GUI reads job progress from the repository
+ Same code in production; only the datasource changes
− Needs a database even for the demo (H2 in memory, so no install)
