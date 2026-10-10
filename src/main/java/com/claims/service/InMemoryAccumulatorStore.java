package com.claims.service;

import com.claims.domain.Accumulator;
import com.claims.domain.AccumulatorScope;
import com.claims.domain.Money;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Accumulators held in memory for one unit of work (one batch run or one API call).
 * Not thread-safe on purpose: a run is processed by one thread, in file order (ADR 0006).
 */
public final class InMemoryAccumulatorStore implements AccumulatorStore {

    private final int benefitYear;
    private final Map<Long, Accumulator> individual = new HashMap<>();
    private final Map<Long, Accumulator> family = new HashMap<>();

    public InMemoryAccumulatorStore(int benefitYear,
                                    Map<Long, BigDecimal> openingIndividual,
                                    Map<Long, BigDecimal> openingFamily) {
        this.benefitYear = benefitYear;
        openingIndividual.forEach((id, amount) ->
                individual.put(id, new Accumulator(id, AccumulatorScope.INDIVIDUAL, benefitYear, amount)));
        openingFamily.forEach((id, amount) ->
                family.put(id, new Accumulator(id, AccumulatorScope.FAMILY, benefitYear, amount)));
    }

    @Override
    public BigDecimal individual(long policyHolderId) {
        Accumulator a = individual.get(policyHolderId);
        return a == null ? Money.ZERO : a.amount();
    }

    @Override
    public BigDecimal family(long policyId) {
        Accumulator a = family.get(policyId);
        return a == null ? Money.ZERO : a.amount();
    }

    @Override
    public void add(long policyHolderId, long policyId, BigDecimal memberPays) {
        individual.merge(policyHolderId,
                Accumulator.zero(policyHolderId, AccumulatorScope.INDIVIDUAL, benefitYear).add(memberPays),
                (current, ignored) -> current.add(memberPays));
        family.merge(policyId,
                Accumulator.zero(policyId, AccumulatorScope.FAMILY, benefitYear).add(memberPays),
                (current, ignored) -> current.add(memberPays));
    }
}
