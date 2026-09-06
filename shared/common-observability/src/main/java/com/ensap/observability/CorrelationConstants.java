package com.ensap.observability;

import java.util.List;

/**
 * Correlation field names (spec §24) and the HTTP headers/MDC keys they map to.
 *
 * <p>{@code traceId} is intentionally excluded from {@link #PROPAGATED_KEYS}: it is populated
 * automatically in MDC by Micrometer Tracing (micrometer-tracing-bridge-otel) once a service adds
 * that dependency — this library does not need to (and must not) manage it.
 */
public final class CorrelationConstants {

    private CorrelationConstants() {
    }

    public static final String CORRELATION_ID = "correlationId";
    public static final String SITE_ID = "siteId";
    public static final String DEPLOYMENT_ID = "deploymentId";
    public static final String WORKFLOW_INSTANCE_ID = "workflowInstanceId";
    public static final String OPERATION_ID = "operationId";

    public static final String HEADER_CORRELATION_ID = "X-Correlation-Id";
    public static final String HEADER_SITE_ID = "X-Site-Id";
    public static final String HEADER_DEPLOYMENT_ID = "X-Deployment-Id";
    public static final String HEADER_WORKFLOW_INSTANCE_ID = "X-Workflow-Instance-Id";
    public static final String HEADER_OPERATION_ID = "X-Operation-Id";

    /** MDC / Reactor-Context keys that {@link CorrelationIdWebFilter} propagates. */
    public static final List<String> PROPAGATED_KEYS = List.of(
            CORRELATION_ID, SITE_ID, DEPLOYMENT_ID, WORKFLOW_INSTANCE_ID, OPERATION_ID);
}
