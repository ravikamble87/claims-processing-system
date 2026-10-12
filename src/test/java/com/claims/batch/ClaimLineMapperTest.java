package com.claims.batch;

import com.claims.domain.Claim;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClaimLineMapperTest {

    private final ClaimLineMapper mapper = new ClaimLineMapper();

    @Test
    void mapsAllSixColumnsAsRawText() {
        Claim claim = mapper.mapLine("100001,1000011,2016-05-06,Inpatient Hospital Care,ROOM AND BOARD,1000.00", 2);

        assertEquals(new Claim(1, "100001", "1000011", "2016-05-06", "Inpatient Hospital Care", "ROOM AND BOARD", "1000.00"),
                claim);
    }

    @Test
    void lineNumberSkipsTheHeader() {
        // FlatFileItemReader counts the header as line 1, so line 20 is claim 19
        assertEquals(19, mapper.mapLine("100007,1000071,2016-07-25,Outpatient Services,X-RAYS,800.00", 20).lineNo());
    }

    @Test
    void trimsSpacesAroundValues() {
        Claim claim = mapper.mapLine(" 100005 , 1000051 ,2016-04-11,  inpatient hospital care , surgery  , 2000.00 ", 2);

        assertEquals("100005", claim.policyId());
        assertEquals("inpatient hospital care", claim.mainCategory());
        assertEquals("surgery", claim.subCategory());
        assertEquals("2000.00", claim.billedAmount());
    }

    @Test
    void missingColumnsBecomeEmptyInsteadOfFailing() {
        // A12: the row must still reach the output, as E0005
        Claim claim = mapper.mapLine("100001,1000011,2016-05-06", 5);

        assertEquals("", claim.mainCategory());
        assertEquals("", claim.subCategory());
        assertEquals("", claim.billedAmount());
    }

    @Test
    void quotedFieldWithComma() {
        Claim claim = mapper.mapLine("100005,1000051,2016-06-08,Outpatient Services,\"MRI, CT, PET\",1200.00", 2);

        assertEquals("MRI, CT, PET", claim.subCategory());
        assertEquals("1200.00", claim.billedAmount());
    }
}
