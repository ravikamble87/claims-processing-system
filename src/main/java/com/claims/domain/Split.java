package com.claims.domain;

import java.math.BigDecimal;
import java.util.Objects;

/** How one bill is divided between the member and the plan. memberPays + planPays always equals billed. */
public record Split(BigDecimal memberPays, BigDecimal planPays, String processingMessage) {

    public Split {
        Objects.requireNonNull(memberPays, "memberPays");
        Objects.requireNonNull(planPays, "planPays");
        Objects.requireNonNull(processingMessage, "processingMessage");
    }
}
