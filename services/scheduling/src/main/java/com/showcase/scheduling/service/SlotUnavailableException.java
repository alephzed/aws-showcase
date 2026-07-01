package com.showcase.scheduling.service;

/**
 * Thrown when a slot cannot be booked because it is missing, already taken, or
 * was taken concurrently (optimistic-lock / unique-constraint conflict). Maps to HTTP 409.
 */
public class SlotUnavailableException extends RuntimeException {

    public SlotUnavailableException(String message) {
        super(message);
    }

    public SlotUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
