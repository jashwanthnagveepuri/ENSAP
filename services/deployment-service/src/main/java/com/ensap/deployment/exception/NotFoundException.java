package com.ensap.deployment.exception;

/**
 * Thrown when a requested deployment doesn't exist. Mapped to 404 by
 * {@link ErrorMapper}.
 */
public class NotFoundException extends RuntimeException {

    private final String code;

    public NotFoundException(String code, String message) {
        super(message);
        this.code = code;
    }

    public static NotFoundException deployment(String deploymentId) {
        return new NotFoundException("DEPLOYMENT_NOT_FOUND", "No deployment with id " + deploymentId);
    }

    public String getCode() {
        return code;
    }
}
