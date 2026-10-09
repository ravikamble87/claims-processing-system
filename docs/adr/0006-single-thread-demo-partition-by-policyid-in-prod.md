# ADR 0006: Single-threaded for the demo; partition by PolicyId for production

Status: Accepted · Date: 2026-10-09

## Context
Claims must be processed in file order, and family accumulators are shared mutable state.
Production volume is ~700,000 claims/day.

## Decision
Demo: one thread, one step. Production: partition by hash(PolicyId), each partition ordered by
input sequence; output exported ORDER BY sequence; family accumulator row locked during update.

## Consequences
+ Demo is correct and easy to explain
+ Production design scales without race conditions on accumulators
− Partitioning is described, not built, in this assignment
