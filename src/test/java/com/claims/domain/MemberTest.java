package com.claims.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A9: covered when start <= date of service <= end; both ends count; no end date = still covered. */
class MemberTest {

    // Jacke Seegers: covered 2009-01-01 .. 2016-05-31
    private final Member jacke = new Member(1000041, 100004, "Jacke", "Seegers", "P003",
            LocalDate.of(2009, 1, 1), LocalDate.of(2016, 5, 31));

    // San Mildred: covered from 2016-01-05, no end date
    private final Member san = new Member(1000061, 100006, "San", "Mildred", "P001",
            LocalDate.of(2016, 1, 5), null);

    @ParameterizedTest
    @CsvSource({
            "2008-12-31, false",
            "2009-01-01, true",     // start date counts
            "2016-05-31, true",     // end date counts
            "2016-06-01, false",
            "2016-06-10, false"
    })
    void coverageWithAnEndDate(LocalDate dateOfService, boolean expected) {
        assertEquals(expected, jacke.isCoveredOn(dateOfService));
    }

    @ParameterizedTest
    @CsvSource({
            "2016-01-02, false",
            "2016-01-04, false",
            "2016-01-05, true",
            "2016-12-31, true"
    })
    void openEndedCoverage(LocalDate dateOfService, boolean expected) {
        assertEquals(expected, san.isCoveredOn(dateOfService));
    }

    @Test
    void endDateBeforeStartDateIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                new Member(1, 1, "A", "B", "P001", LocalDate.of(2016, 5, 1), LocalDate.of(2016, 4, 30)));
        assertTrue(e.getMessage().contains("coverageEnd before coverageStart"));
    }

    @Test
    void sameStartAndEndDateIsOneCoveredDay() {
        Member oneDay = new Member(1, 1, "A", "B", "P001", LocalDate.of(2016, 3, 1), LocalDate.of(2016, 3, 1));

        assertTrue(oneDay.isCoveredOn(LocalDate.of(2016, 3, 1)));
    }
}
