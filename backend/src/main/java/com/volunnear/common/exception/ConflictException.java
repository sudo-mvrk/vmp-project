package com.volunnear.common.exception;

/** The request is valid but the resource is in a state that does not allow it (409). */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
