package com.ensap.siteprofile.exception;

import java.util.List;

/**
 * Thrown when a request body fails bean validation. Mapped to 400 by
 * {@link ErrorMapper}, joining {@link #getViolations()} into the message.
 */
public class ValidationException extends RuntimeException {

    private final List<String> violations;

    public ValidationException(List<String> violations) {
        super(String.join("; ", violations));
        this.violations = violations;
    }

    public List<String> getViolations() {
        return violations;
    }
}
