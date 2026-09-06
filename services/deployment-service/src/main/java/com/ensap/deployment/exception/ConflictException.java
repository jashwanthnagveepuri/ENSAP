package com.ensap.deployment.exception;

/**
 * Thrown when a deployment isn't in a state that allows the requested
 * transition (e.g. retrying a RUNNING deployment, cancelling a COMPLETED
 * one). Mapped to 409 by {@link ErrorMapper}.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
