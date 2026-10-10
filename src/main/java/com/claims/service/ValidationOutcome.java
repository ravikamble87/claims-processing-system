package com.claims.service;

import com.claims.domain.Claim;
import com.claims.domain.CoverageRule;
import com.claims.domain.ErrorCode;
import com.claims.domain.Member;
import com.claims.domain.Plan;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Result of ClaimValidator: either a fully parsed and resolved claim, or the first error found. */
public sealed interface ValidationOutcome {

    record Valid(Claim claim,
                 long policyId,
                 long policyHolderId,
                 LocalDate dateOfService,
                 BigDecimal billedAmount,
                 Member member,
                 Plan plan,
                 CoverageRule rule) implements ValidationOutcome {
    }

    record Invalid(ErrorCode errorCode) implements ValidationOutcome {
    }
}
