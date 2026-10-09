# 01 · Problem and glossary

## The problem in one paragraph

Each claim (a medical bill) must be split between the member and the plan. Before each claim,
check whether the member's **individual** accumulated deductible or the **family** accumulated
deductible has reached its annual limit. Then apply the coverage rule for that service:
X% AFTER DEDUCTIBLE, No Charge, or a flat dollar amount. Process claims in file order; error
rows keep their position.

## Glossary

| Term | Meaning | Source |
| --- | --- | --- |
| Plan | A rulebook: deductibles + coverage rules (P001, P002, P003) | PlanDescriptions |
| Premium | Monthly fee; not used in claim math | PlanDescriptions |
| Individual annual deductible | What one member pays themselves each year before the plan shares cost | PlanDescriptions |
| Family annual deductible | Same, for the whole family combined | PlanDescriptions |
| Policy / PolicyId | The family's policy; one PolicyId = one family | PolicyData |
| Policy holder (member) | One person on the policy | PolicyData |
| Accumulator | Year-to-date amount paid by the member (individual) or family | PolicyData H, I |
| Claim | One bill submitted for payment | SampleTransactions |
| Adjudication | Deciding who pays what for a claim | Industry term |
| X% AFTER DEDUCTIBLE | Plan pays X% once the deductible is met; before that, member pays 100% | PlanCoverage |
| No Charge | Plan pays 100%, regardless of deductible | PlanCoverage |
| $N flat | Plan pays exactly $N, member pays the rest, regardless of deductible | PlanCoverage |

TODO (you): add any terms you had to look up while building.

## Worked scenarios (from the sample data)

| Scenario | Sample row | Result |
| --- | --- | --- |
| Not met → member pays all | 1 (Sam, $1,000) | Member 1,000 / Plan 0 |
| Crosses limit, no split | 3 (Sam, $4,000 surgery, 2,000 → 6,000) | Member 4,000 / Plan 0 |
| Individual met | 4 (Sam at 6,000) | Member 600 / Plan 400 |
| Family met, individual not | 14 (Jina, family 12,000) | Member 96.24 / Plan 144.36 |
| No Charge | 15 (Daniel, physical) | Member 0 / Plan 350 |
| Flat dollar | 18 (Mack, urgent care $250, P003) | Member 130 / Plan 120 |
| Unknown member | 7 (1000016) | E0001 |
