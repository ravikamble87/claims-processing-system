package com.claims.refdata;

import com.claims.domain.Member;
import com.claims.domain.Plan;
import com.claims.service.AccumulatorStore;
import com.claims.service.InMemoryAccumulatorStore;
import com.claims.service.MemberRepository;
import com.claims.service.PlanRepository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * Plans, members and opening accumulators loaded once at start-up. Immutable, so safe to share.
 * Every batch run gets a fresh accumulator store seeded from the opening values (assumption A24),
 * so running the same file twice gives the same output.
 */
public record ReferenceData(
        Map<String, Plan> plans,
        Map<Long, Member> members,
        Map<Long, BigDecimal> openingIndividual,
        Map<Long, BigDecimal> openingFamily) implements PlanRepository, MemberRepository {

    public ReferenceData {
        plans = Map.copyOf(plans);
        members = Map.copyOf(members);
        openingIndividual = Map.copyOf(openingIndividual);
        openingFamily = Map.copyOf(openingFamily);
    }

    @Override
    public Optional<Plan> findPlan(String planId) {
        return Optional.ofNullable(plans.get(planId));
    }

    @Override
    public Optional<Member> findMember(long policyHolderId) {
        return Optional.ofNullable(members.get(policyHolderId));
    }

    public AccumulatorStore newAccumulatorStore(int benefitYear) {
        return new InMemoryAccumulatorStore(benefitYear, openingIndividual, openingFamily);
    }
}
