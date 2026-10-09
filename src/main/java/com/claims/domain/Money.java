package com.claims.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Money helpers. All amounts are BigDecimal with scale 2, rounded HALF_UP (assumption A6).
 * Never build money from a double: new BigDecimal(240.6) carries binary error.
 */
public final class Money {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE, ROUNDING);
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private Money() {
    }

    /** Parses "1000", "1000.0", "240.60" into a scale-2 amount. Throws NumberFormatException on bad input. */
    public static BigDecimal of(String text) {
        return of(new BigDecimal(text.trim()));
    }

    public static BigDecimal of(BigDecimal value) {
        return value.setScale(SCALE, ROUNDING);
    }

    /** amount x percent / 100, rounded to cents. percentOf(240.60, 60) = 144.36 */
    public static BigDecimal percentOf(BigDecimal amount, BigDecimal percent) {
        return amount.multiply(percent).divide(HUNDRED, SCALE, ROUNDING);
    }
}
