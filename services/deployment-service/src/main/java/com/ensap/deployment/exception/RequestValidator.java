package com.ensap.deployment.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Functional endpoints don't get the automatic {@code @Valid} handling
 * annotated controllers do, so handlers call this explicitly on a decoded
 * request body before passing it to a service (docs/10-api-design.md
 * "validation").
 */
@Component
public class RequestValidator {

    private final Validator validator;

    public RequestValidator(Validator validator) {
        this.validator = validator;
    }

    public <T> T validate(T value) {
        Set<ConstraintViolation<T>> violations = validator.validate(value);
        if (!violations.isEmpty()) {
            throw new ValidationException(violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.toList()));
        }
        return value;
    }
}
