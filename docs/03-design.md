# 03 · Design

## Principles

- **Domain core has no Spring dependency** (ADR 0005): the money math is plain Java and unit-testable in milliseconds.
- **One rulebook, three doors**: GUI, CLI and REST all call the same `ClaimAdjudicator`.
- **Errors are results, not exceptions**: keeps error rows in their original position.

## Class diagram

![Class diagram](diagrams/class-diagram.png)

Source: [diagrams/class-diagram.mmd](diagrams/class-diagram.mmd)

## Packages

```text
com.claims
  domain     Plan, Member, CoverageKey, CoverageRule (+3), DeductibleStatus, Split,
             Accumulator, Claim, AdjudicationResult, ErrorCode, Money
  service    ClaimValidator, DeductibleService, ClaimAdjudicator
  refdata    CSV loaders, PlanRepository, PolicyRepository, AccumulatorStore
  batch      Job config, reader, processor, writer
  api        ClaimController, DeductibleController, error handler
  web        GUI page + job progress endpoint
  cli        CommandLineRunner
```

## Flow of one claim (sample row 14)

1. Reader builds `Claim` (raw strings, lineNo 14).
2. `ClaimValidator` → valid.
3. `Plan P001.ruleFor("Prescription Drugs", "GENERIC")` → `PercentAfterDeductibleRule(60)`.
4. `DeductibleService.status(P001, 467.60, 12000.00)` → family met.
5. Split: plan 144.36, member 96.24.
6. Both accumulators += 96.24 → 563.84 / 12,096.24.
7. `AdjudicationResult` written in position 14.

## Design patterns used

| Pattern | Where | Why |
| --- | --- | --- |
| Strategy | `CoverageRule` implementations | New rule type = new class, no if/else chain |
| Sealed hierarchy | `CoverageRule` | Compiler enforces all rule types are handled |
| Ports and adapters | `refdata` interfaces | Swap CSV for Oracle without touching domain |
| Factory method | `CoverageRule.parse()` | Rule text → rule object in one place |

TODO (you): update this file whenever the structure changes.
