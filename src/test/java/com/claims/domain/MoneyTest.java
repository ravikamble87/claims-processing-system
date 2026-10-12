package com.claims.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static com.claims.TestData.money;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** A6: every amount is scale 2, rounded HALF_UP. */
class MoneyTest {

    @ParameterizedTest
    @CsvSource({
            "1000,     1000.00",
            "240.6,    240.60",
            "' 25.6 ', 25.60",
            "0.005,    0.01",
            "0.004,    0.00"
    })
    void ofParsesTextToTwoDecimals(String text, BigDecimal expected) {
        assertEquals(expected, Money.of(text));
    }

    @Test
    void ofRejectsText() {
        assertThrows(NumberFormatException.class, () -> Money.of("abc"));
    }

    @Test
    void zeroHasTwoDecimals() {
        assertEquals(money("0.00"), Money.ZERO);
    }

    @ParameterizedTest
    @CsvSource({
            "240.60, 60,  144.36",   // sample row 14
            "1000.00, 40, 400.00",
            "33.33,  60,  20.00",    // 19.998 rounds up
            "99.99,  40,  40.00",    // 39.996 rounds up
            "0.01,   60,  0.01",     // 0.006 rounds up
            "0.01,   40,  0.00",     // 0.004 rounds down
            "500.00, 100, 500.00",
            "500.00, 0,   0.00"
    })
    void percentOfRoundsToCents(BigDecimal amount, BigDecimal percent, BigDecimal expected) {
        assertEquals(expected, Money.percentOf(amount, percent));
    }
}
