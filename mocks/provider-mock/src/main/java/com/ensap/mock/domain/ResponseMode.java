package com.ensap.mock.domain;

/**
 * The discrete outcome a provider mock returns for a job request, one knob
 * of the controllable behavior set in master spec §22. Latency, temporary
 * outage, random-failure percentage, rate limiting and duplicate callback
 * are independent knobs layered on top (see {@link ProviderBehavior}).
 */
public enum ResponseMode {
    SUCCESS(200),
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    NOT_FOUND(404),
    CONFLICT(409),
    RATE_LIMITED(429),
    SERVER_ERROR(500),
    SERVICE_UNAVAILABLE(503),
    TIMEOUT(0);

    private final int httpStatus;

    ResponseMode(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
