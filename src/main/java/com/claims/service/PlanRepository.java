package com.claims.service;

import com.claims.domain.Plan;

import java.util.Optional;

/** Port: where plans come from (CSV for the demo, the Plan database in production). */
public interface PlanRepository {

    Optional<Plan> findPlan(String planId);
}
