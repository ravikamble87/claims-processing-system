package com.claims.api;

import com.claims.domain.DeductibleStatus;
import com.claims.domain.Plan;
import com.claims.service.DeductibleService;
import com.claims.service.PlanRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * The reusable accumulated-deductible service, exposed on its own for other internal and external systems,
 * as the assignment asks. Same DeductibleService the claim flow uses.
 */
@RestController
@RequestMapping("/api/v1/deductible")
public class DeductibleController {

    private final DeductibleService deductibleService;
    private final PlanRepository plans;

    public DeductibleController(DeductibleService deductibleService, PlanRepository plans) {
        this.deductibleService = deductibleService;
        this.plans = plans;
    }

    @PostMapping("/status")
    public DeductibleResponse status(@Valid @RequestBody DeductibleRequest request) {
        Plan plan = plans.findPlan(request.planId().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown plan " + request.planId()));
        DeductibleStatus status = deductibleService.status(plan, request.individualAccumulated(), request.familyAccumulated());
        return new DeductibleResponse(plan.planId(), plan.individualDeductible(), plan.familyDeductible(),
                request.individualAccumulated(), request.familyAccumulated(),
                status.individualMet(), status.familyMet(), status.isMet());
    }
}
