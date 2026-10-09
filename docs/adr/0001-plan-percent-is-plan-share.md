# ADR 0001: The coverage percentage is the plan's share

Status: Accepted · Date: 2026-10-09

## Context
In US plans, "40% coinsurance" usually means the member pays 40%. The assignment says
"60% AFTER DEDUCTIBLE → plan pays 60%".

## Decision
The percentage in PlanCoverage is what the **plan** pays. Member pays the remainder.

## Consequences
+ Matches every expected row (row 4: $1,000 at 40% → plan 400)
− Must be stated up front in the interview; anyone with US insurance background will read it the other way
