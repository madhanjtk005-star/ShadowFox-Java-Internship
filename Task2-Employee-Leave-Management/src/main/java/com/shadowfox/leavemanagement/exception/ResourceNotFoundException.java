package com.shadowfox.leavemanagement.exception;

/** Entity not found -> 404. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
