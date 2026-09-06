package com.ensap.mock.domain;

/**
 * The full controllable-behavior configuration for one provider (master
 * spec §22), doubling as the Admin/Provider-Simulation request+response
 * body. Immutable — the admin API swaps the whole value in one atomic
 * replace (see {@code ProviderBehaviorStore}), so there is never a
 * partially-applied update visible to concurrent requests.
 */
public record ProviderBehavior(
        ResponseMode responseMode,
        long fixedLatencyMs,
        long randomLatencyMinMs,
        long randomLatencyMaxMs,
        boolean outageEnabled,
        int randomFailurePercent,
        boolean duplicateCallback,
        int rateLimitPerMinute
) {
    public static ProviderBehavior defaults() {
        return new ProviderBehavior(ResponseMode.SUCCESS, 0, 0, 0, false, 0, false, 0);
    }

    /** Null/negative-safe validation for the admin PUT endpoint. */
    public String validate() {
        if (responseMode == null) {
            return "responseMode is required";
        }
        if (fixedLatencyMs < 0 || randomLatencyMinMs < 0 || randomLatencyMaxMs < 0) {
            return "latency values must be >= 0";
        }
        if (randomLatencyMaxMs < randomLatencyMinMs) {
            return "randomLatencyMaxMs must be >= randomLatencyMinMs";
        }
        if (randomFailurePercent < 0 || randomFailurePercent > 100) {
            return "randomFailurePercent must be between 0 and 100";
        }
        if (rateLimitPerMinute < 0) {
            return "rateLimitPerMinute must be >= 0";
        }
        return null;
    }
}
