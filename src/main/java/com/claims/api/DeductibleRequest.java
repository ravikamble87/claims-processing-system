package com.claims.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DeductibleRequest(
        @NotBlank String planId,
        @NotNull @DecimalMin("0.00") BigDecimal individualAccumulated,
        @NotNull @DecimalMin("0.00") BigDecimal familyAccumulated) {
}
