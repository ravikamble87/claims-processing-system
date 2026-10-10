package com.claims.api;

import com.claims.config.ClaimsProperties;
import com.claims.domain.AdjudicationResult;
import com.claims.domain.Claim;
import com.claims.domain.ErrorCode;
import com.claims.service.ClaimAdjudicator;
import com.claims.service.InMemoryAccumulatorStore;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Real-time mode (door C): one claim in, one result out. Stateless: nothing is saved (assumption A17).
 * 200 = processed; 422 = rejected by a business rule (body carries the error code); 500 = unexpected error (E0099).
 */
@RestController
@RequestMapping("/api/v1/claims")
public class ClaimController {

    private final ClaimAdjudicator adjudicator;
    private final ClaimsProperties properties;

    public ClaimController(ClaimAdjudicator adjudicator, ClaimsProperties properties) {
        this.adjudicator = adjudicator;
        this.properties = properties;
    }

    @PostMapping("/adjudicate")
    public ResponseEntity<ClaimResponse> adjudicate(@Valid @RequestBody ClaimRequest request) {
        Claim claim = new Claim(1, request.policyId(), request.policyHolderId(), request.dateOfService(),
                request.mainCategory(), request.subCategory(), request.billedAmount());

        // Seed a throwaway store with the caller's totals. If an id is malformed the store stays empty;
        // ClaimValidator rejects the claim (E0005 / E0006) before the store is read.
        Map<Long, BigDecimal> individual = new HashMap<>();
        Map<Long, BigDecimal> family = new HashMap<>();
        Long memberId = parseIdOrNull(request.policyHolderId());
        Long policyId = parseIdOrNull(request.policyId());
        if (memberId != null && policyId != null) {
            individual.put(memberId, request.individualAccumulated());
            family.put(policyId, request.familyAccumulated());
        }
        var store = new InMemoryAccumulatorStore(properties.benefitYear(), individual, family);

        AdjudicationResult result = adjudicator.adjudicate(claim, store);
        HttpStatus status = !result.isError() ? HttpStatus.OK
                : result.errorCode() == ErrorCode.E0099 ? HttpStatus.INTERNAL_SERVER_ERROR
                : HttpStatus.UNPROCESSABLE_CONTENT;
        return ResponseEntity.status(status).body(ClaimResponse.from(result));
    }

    private static Long parseIdOrNull(String text) {
        try {
            return text == null ? null : Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
