package com.shadowfox.leavemanagement.exception;

/** Caller lacks the required role / ownership -> 403. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
