# ADR 0003: BigDecimal, round plan share HALF_UP, member pays the remainder

Status: Accepted · Date: 2026-10-09

## Context
Money must be exact. 240.60 × 60% = 144.36 exactly, but other bills produce fractions of a cent.

## Decision
All amounts are `BigDecimal` scale 2, built from strings. Plan share = round(billed × %, HALF_UP).
Member share = billed − plan share.

## Consequences
+ Shares always sum exactly to the billed amount
+ No floating-point drift across 700,000 claims
− Any rounding difference falls on the member; confirm with the business in production
