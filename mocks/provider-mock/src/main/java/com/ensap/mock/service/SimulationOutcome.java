package com.ensap.mock.service;

import com.ensap.mock.dto.JobResponse;

/** Result of running a provider's configured behavior against one request. */
public sealed interface SimulationOutcome {
    record Success(JobResponse body) implements SimulationOutcome {}
    record Failure(int status, String code, String message) implements SimulationOutcome {}
}
