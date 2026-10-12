package com.claims.service;

import com.claims.TestData;
import com.claims.domain.Claim;
import com.claims.domain.ErrorCode;
import com.claims.domain.Member;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.util.Optional;

import static com.claims.TestData.claim;
import static com.claims.TestData.money;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Every error code, and the validation order: E0005, E0006, E0008, E0007, E0009, E0001, E0002, E0003, E0010, E0004.
 * Processing date 2016-12-31, benefit year 2016 (A10, A11).
 */
class ClaimValidatorTest {

    private final ClaimValidator validator = TestData.validator();

    @Test
    void validClaimIsParsedAndResolved() {
        Claim claim = claim("100001", "1000011", "2016-05-06", "Inpatient Hospital Care", "ROOM AND BOARD", "1000");

        ValidationOutcome.Valid valid = assertInstanceOf(ValidationOutcome.Valid.class, validator.validate(claim));

        assertEquals(100001L, valid.policyId());
        assertEquals(1000011L, valid.policyHolderId());
        assertEquals(LocalDate.of(2016, 5, 6), valid.dateOfService());
        assertEquals(money("1000.00"), valid.billedAmount());
        assertEquals("Sam", valid.member().firstName());
        assertEquals("P001", valid.plan().planId());
        assertEquals("40% AFTER DEDUCTIBLE", valid.rule().ruleText());
        assertEquals(claim, valid.claim());
    }

    @ParameterizedTest(name = "{7}: {0}")
    @CsvSource({
            // E0005 - required field missing (empty or blank)
            "missing policy id,          ,       1000011, 2016-05-06, Inpatient Hospital Care, ROOM AND BOARD, 1000.00, E0005",
            "missing policy holder id,   100001, ,        2016-05-06, Inpatient Hospital Care, ROOM AND BOARD, 1000.00, E0005",
            "missing date,               100001, 1000011, ,           Inpatient Hospital Care, ROOM AND BOARD, 1000.00, E0005",
            "missing main category,      100001, 1000011, 2016-05-06, ,                        ROOM AND BOARD, 1000.00, E0005",
            "missing sub category,       100001, 1000011, 2016-05-06, Inpatient Hospital Care, '',             1000.00, E0005",
            "blank billed amount,        100001, 1000011, 2016-05-06, Inpatient Hospital Care, ROOM AND BOARD, '   ',   E0005",
            // E0006 - malformed values
            "amount is text,             100001, 1000011, 2016-05-06, Inpatient Hospital Care, ROOM AND BOARD, abc,     E0006",
            "amount has 3 decimals,      100001, 1000011, 2016-05-06, Inpatient Hospital Care, ROOM AND BOARD, 10.555,  E0006",
            "impossible date,            100001, 1000011, 2016-13-45, Inpatient Hospital Care, ROOM AND BOARD, 1000.00, E0006",
            "date with slashes reversed, 100001, 1000011, 2016/05/06, Inpatient Hospital Care, ROOM AND BOARD, 1000.00, E0006",
            "US date that does not exist,100001, 1000011, 2/30/2016,  Inpatient Hospital Care, ROOM AND BOARD, 1000.00, E0006",
            "holder id is text,          100001, ABC123,  2016-05-06, Inpatient Hospital Care, ROOM AND BOARD, 1000.00, E0006",
            "holder id is zero,          100001, 0,       2016-05-06, Inpatient Hospital Care, ROOM AND BOARD, 1000.00, E0006",
            "policy id is negative,      -5,     1000011, 2016-05-06, Inpatient Hospital Care, ROOM AND BOARD, 1000.00, E0006",
            // E0008 - amount not positive
            "zero amount,                100001, 1000011, 2016-05-06, Inpatient Hospital Care, ROOM AND BOARD, 0.00,    E0008",
            "negative amount,            100001, 1000011, 2016-05-06, Inpatient Hospital Care, ROOM AND BOARD, -50,     E0008",
            // E0007 / E0009 - dates
            "after processing date,      100001, 1000011, 2017-01-15, Inpatient Hospital Care, ROOM AND BOARD, 1000.00, E0007",
            "before benefit year,        100001, 1000011, 2015-12-20, Inpatient Hospital Care, ROOM AND BOARD, 1000.00, E0009",
            // E0001 / E0002 / E0003 - member and coverage
            "unknown member,             100001, 1000016, 2016-10-12, Prescription Drugs,      GENERIC,        61.40,   E0001",
            "unknown policy and member,  999999, 9999999, 2016-06-01, Outpatient Services,     X-RAYS,         100.00,  E0001",
            "member on wrong policy,     100002, 1000011, 2016-06-02, Outpatient Services,     X-RAYS,         100.00,  E0002",
            "after coverage end,         100004, 1000041, 2016-06-10, Outpatient Services,     X-RAYS,         300.00,  E0003",
            "before coverage start,      100006, 1000061, 2016-01-02, Outpatient Services,     X-RAYS,         300.00,  E0003",
            "day after Mack's end date,  100007, 1000071, 2016-09-02, Outpatient Services,     X-RAYS,         300.00,  E0003",
            // E0004 - no rule for main + sub
            "unknown sub category,       100005, 1000051, 2016-06-03, Outpatient Services,     TEETH CLEANING, 150.00,  E0004",
            "sub under wrong main,       100005, 1000051, 2016-06-03, Preventive Care,         X-RAYS,         150.00,  E0004",
            "unknown main category,      100005, 1000051, 2016-06-03, Dental Care,             ROUTINE CLEANING,150.00, E0004",
            "full grouped label,         100005, 1000051, 2016-06-08, Outpatient Services,     'MRI, CT, PET', 1200.00, E0004"
    })
    void rejectsInvalidClaim(String description, String policyId, String policyHolderId, String dateOfService,
                             String mainCategory, String subCategory, String billedAmount, ErrorCode expected) {
        Claim claim = claim(policyId, policyHolderId, dateOfService, mainCategory, subCategory, billedAmount);

        ValidationOutcome.Invalid invalid = assertInstanceOf(ValidationOutcome.Invalid.class, validator.validate(claim),
                description);
        assertEquals(expected, invalid.errorCode(), description);
    }

    @ParameterizedTest(name = "{7}: {0}")
    @CsvSource({
            "missing date beats unknown member,    100001, 1000099, ,           Outpatient Services, X-RAYS,         100.00, E0005",
            "missing category beats bad amount,    100001, 1000011, 2016-05-06, ,                    X-RAYS,         abc,    E0005",
            "bad amount beats unknown member,      100001, 1000099, 2016-05-06, Outpatient Services, X-RAYS,         abc,    E0006",
            "zero amount beats future date,        100005, 1000051, 2017-02-01, Outpatient Services, X-RAYS,         0,      E0008",
            "future beats outside benefit year,    100005, 1000051, 2017-01-15, Outpatient Services, X-RAYS,         100.00, E0007",
            "outside year beats unknown member,    100005, 1000099, 2015-12-20, Outpatient Services, X-RAYS,         100.00, E0009",
            "unknown member beats bad category,    100005, 1000099, 2016-06-01, Dental Care,         CLEANING,       100.00, E0001",
            "wrong policy beats expired coverage,  100001, 1000041, 2016-06-10, Outpatient Services, X-RAYS,         100.00, E0002",
            "expired coverage beats bad category,  100004, 1000041, 2016-06-10, Dental Care,         CLEANING,       100.00, E0003"
    })
    void firstFailureWins(String description, String policyId, String policyHolderId, String dateOfService,
                          String mainCategory, String subCategory, String billedAmount, ErrorCode expected) {
        Claim claim = claim(policyId, policyHolderId, dateOfService, mainCategory, subCategory, billedAmount);

        ValidationOutcome.Invalid invalid = assertInstanceOf(ValidationOutcome.Invalid.class, validator.validate(claim),
                description);
        assertEquals(expected, invalid.errorCode(), description);
    }

    @ParameterizedTest
    @CsvSource({
            "100004, 1000041, 2016-05-31",   // Jacke: last covered day
            "100006, 1000061, 2016-01-05",   // San: first covered day
            "100007, 1000071, 2016-09-01",   // Mack: last covered day
            "100005, 1000051, 2016-12-31",   // processing date itself is not "future"
            "100005, 1000051, 2016-01-01"    // first day of the benefit year
    })
    void boundaryDatesAreAccepted(String policyId, String policyHolderId, String dateOfService) {
        Claim claim = claim(policyId, policyHolderId, dateOfService, "Outpatient Services", "X-RAYS", "300.00");

        assertInstanceOf(ValidationOutcome.Valid.class, validator.validate(claim));
    }

    @Test
    void acceptsUsStyleDates() {
        // A18: Excel often exports 5/6/2016
        Claim claim = claim("100001", "1000011", "5/6/2016", "Inpatient Hospital Care", "ROOM AND BOARD", "1000.00");

        ValidationOutcome.Valid valid = assertInstanceOf(ValidationOutcome.Valid.class, validator.validate(claim));
        assertEquals(LocalDate.of(2016, 5, 6), valid.dateOfService());
    }

    @Test
    void matchesCategoriesIgnoringCaseAndSpaces() {
        // A8
        Claim claim = claim(" 100005 ", " 1000051 ", " 2016-04-11 ", "  inpatient hospital care ", " surgery  ", " 2000 ");

        ValidationOutcome.Valid valid = assertInstanceOf(ValidationOutcome.Valid.class, validator.validate(claim));
        assertEquals("40% AFTER DEDUCTIBLE", valid.rule().ruleText());
        assertEquals(money("2000.00"), valid.billedAmount());
    }

    @Test
    void groupedSubCategoriesMatchOneByOne() {
        // PlanCoverage groups "MRI, CT, PET" and "ROOM AND BOARD, SURGERY, ..."; coverage.csv has one row per service.
        for (String sub : new String[]{"MRI", "CT", "PET"}) {
            Claim claim = claim("100005", "1000051", "2016-06-09", "Outpatient Services", sub, "900.00");
            assertInstanceOf(ValidationOutcome.Valid.class, validator.validate(claim), sub);
        }
        for (String sub : new String[]{"ROOM AND BOARD", "SURGERY", "ANESTHESIA", "X-RAYS", "LAB TESTS", "MEDICATIONS", "MENTAL HEALTH CARE"}) {
            Claim claim = claim("100005", "1000051", "2016-06-09", "Inpatient Hospital Care", sub, "900.00");
            assertInstanceOf(ValidationOutcome.Valid.class, validator.validate(claim), sub);
        }
    }

    @Test
    void memberWhosePlanIsNotConfigured() {
        // E0010 cannot happen with the real data (the loader refuses it), so use a member on an unknown plan.
        Member orphan = new Member(2000011, 200001, "Test", "Member", "P999", LocalDate.of(2016, 1, 1), null);
        MemberRepository members = id -> id == orphan.policyHolderId() ? Optional.of(orphan) : Optional.empty();
        PlanRepository plans = TestData.referenceData();
        ClaimValidator validator = new ClaimValidator(plans, members, TestData.PROCESSING_DATE, TestData.BENEFIT_YEAR);

        Claim claim = claim("200001", "2000011", "2016-06-01", "Outpatient Services", "X-RAYS", "100.00");

        ValidationOutcome.Invalid invalid = assertInstanceOf(ValidationOutcome.Invalid.class, validator.validate(claim));
        assertEquals(ErrorCode.E0010, invalid.errorCode());
    }
}
