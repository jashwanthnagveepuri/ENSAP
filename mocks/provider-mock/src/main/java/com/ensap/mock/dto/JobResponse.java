package com.ensap.mock.dto;

import java.time.Instant;

/** Success body returned by a provider job endpoint. */
public record JobResponse(String jobId, String providerType, String status, Instant receivedAt, String message) {
}
