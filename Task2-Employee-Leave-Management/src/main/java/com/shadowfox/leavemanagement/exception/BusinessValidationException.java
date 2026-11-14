package com.shadowfox.leavemanagement.exception;

/** Business rule violated (overlap, insufficient balance, wrong state...) -> 400. */
public class BusinessValidationException extends RuntimeException {
    public BusinessValidationException(String message) {
        super(message);
    }
}
