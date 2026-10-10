package com.claims.service;

import java.math.BigDecimal;

/**
 * Port for year-to-date deductible accumulators (the "notebook").
 * Batch mode uses one store per job run, seeded from PolicyData; the REST API uses a throwaway store
 * seeded from the request (assumption A17). In production this would be a database table with a row lock
 * per family (see docs/04-scaling-and-deployment.md).
 */
public interface AccumulatorStore {

    BigDecimal individual(long policyHolderId);

    BigDecimal family(long policyId);

    /** Adds the member's share to both the individual and the family accumulator (assumption A3). */
    void add(long policyHolderId, long policyId, BigDecimal memberPays);
}
