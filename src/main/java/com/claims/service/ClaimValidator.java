package com.claims.service;

import com.claims.domain.Claim;
import com.claims.domain.CoverageRule;
import com.claims.domain.ErrorCode;
import com.claims.domain.Member;
import com.claims.domain.Money;
import com.claims.domain.Plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Parses the raw claim strings and checks them in a fixed order; the first failure wins
 * (docs/02-assumptions.md, "Validation order"):
 * E0005, E0006, E0008, E0007, E0009, E0001, E0002, E0003, E0010, E0004.
 * Never throws for bad input and never touches accumulators (assumption A12).
 */
public final class ClaimValidator {

    /** ISO dates, plus US-style dates that Excel often writes when exporting CSV (assumption A18). */
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("M/d/uuuu").withResolverStyle(ResolverStyle.STRICT));

    private final PlanRepository plans;
    private final MemberRepository members;
    private final LocalDate processingDate;
    private final int benefitYear;

    public ClaimValidator(PlanRepository plans, MemberRepository members, LocalDate processingDate, int benefitYear) {
        this.plans = Objects.requireNonNull(plans, "plans");
        this.members = Objects.requireNonNull(members, "members");
        this.processingDate = Objects.requireNonNull(processingDate, "processingDate");
        this.benefitYear = benefitYear;
    }

    public ValidationOutcome validate(Claim claim) {
        // E0005: every input column is required
        if (isBlank(claim.policyId()) || isBlank(claim.policyHolderId()) || isBlank(claim.dateOfService())
                || isBlank(claim.mainCategory()) || isBlank(claim.subCategory()) || isBlank(claim.billedAmount())) {
            return invalid(ErrorCode.E0005);
        }

        // E0006: values that cannot be parsed
        Optional<Long> policyId = parseId(claim.policyId());
        Optional<Long> policyHolderId = parseId(claim.policyHolderId());
        Optional<LocalDate> dateOfService = parseDate(claim.dateOfService());
        Optional<BigDecimal> billed = parseAmount(claim.billedAmount());
        if (policyId.isEmpty() || policyHolderId.isEmpty() || dateOfService.isEmpty() || billed.isEmpty()) {
            return invalid(ErrorCode.E0006);
        }

        // E0008: amount must be positive
        if (billed.get().signum() <= 0) {
            return invalid(ErrorCode.E0008);
        }

        // E0007 / E0009: date checks (assumptions A10, A11)
        LocalDate date = dateOfService.get();
        if (date.isAfter(processingDate)) {
            return invalid(ErrorCode.E0007);
        }
        if (date.getYear() != benefitYear) {
            return invalid(ErrorCode.E0009);
        }

        // E0001 / E0002 / E0003: who is the member, and were they covered that day (assumptions A9, A13)
        Optional<Member> member = members.findMember(policyHolderId.get());
        if (member.isEmpty()) {
            return invalid(ErrorCode.E0001);
        }
        if (member.get().policyId() != policyId.get()) {
            return invalid(ErrorCode.E0002);
        }
        if (!member.get().isCoveredOn(date)) {
            return invalid(ErrorCode.E0003);
        }

        // E0010 / E0004: the plan and the rule for this service (assumption A8)
        Optional<Plan> plan = plans.findPlan(member.get().planId());
        if (plan.isEmpty()) {
            return invalid(ErrorCode.E0010);
        }
        Optional<CoverageRule> rule = plan.get().ruleFor(claim.mainCategory(), claim.subCategory());
        if (rule.isEmpty()) {
            return invalid(ErrorCode.E0004);
        }

        return new ValidationOutcome.Valid(claim, policyId.get(), policyHolderId.get(), date, billed.get(),
                member.get(), plan.get(), rule.get());
    }

    private static ValidationOutcome invalid(ErrorCode code) {
        return new ValidationOutcome.Invalid(code);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static Optional<Long> parseId(String text) {
        try {
            long id = Long.parseLong(text.trim());
            return id > 0 ? Optional.of(id) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static Optional<LocalDate> parseDate(String text) {
        for (DateTimeFormatter format : DATE_FORMATS) {
            try {
                return Optional.of(LocalDate.parse(text.trim(), format));
            } catch (DateTimeParseException ignored) {
                // try the next accepted format
            }
        }
        return Optional.empty();
    }

    /** Accepts at most 2 decimal places: "250", "250.5", "250.50". Rejects "250.505" and "abc". */
    private static Optional<BigDecimal> parseAmount(String text) {
        try {
            BigDecimal value = new BigDecimal(text.trim());
            if (value.stripTrailingZeros().scale() > Money.SCALE) {
                return Optional.empty();
            }
            return Optional.of(Money.of(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
