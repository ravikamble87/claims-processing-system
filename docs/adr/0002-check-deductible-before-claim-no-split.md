# ADR 0002: Check the deductible before the claim; never split a claim

Status: Accepted · Date: 2026-10-09

## Context
Row 3: Sam's $4,000 surgery takes him from $2,000 to $6,000 (limit $6,000). Should the part above
the limit get the plan percentage?

## Options considered
1. Evaluate once before the claim — simple, deterministic
2. Split the claim at the threshold — closer to some real plans, more complex

## Decision
Option 1. Status is computed from accumulators before the claim; `>=` means met.

## Consequences
+ Matches all 19 expected rows
+ One comparison per claim
− A split-at-threshold plan would need a new rule type
