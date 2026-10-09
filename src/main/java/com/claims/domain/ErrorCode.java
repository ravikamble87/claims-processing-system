package com.claims.domain;

/**
 * Validation and processing errors (Step 3 of the design doc).
 * Only E0001 is given by the assignment; the rest are our own design.
 */
public enum ErrorCode {

    E0001("Policy holder does not exist"),
    E0002("Policy holder does not belong to this policy"),
    E0003("Coverage not active on date of service"),
    E0004("Invalid coverage category or sub category"),
    E0005("Required field is missing"),
    E0006("Field is malformed"),
    E0007("Date of service is in the future"),
    E0008("Billed amount must be greater than zero"),
    E0009("Date of service outside benefit year"),
    E0010("Plan not configured"),
    E0099("Unexpected processing error");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }

    public String code() {
        return name();
    }

    public String message() {
        return message;
    }
}
