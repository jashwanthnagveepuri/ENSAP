package com.ensap.observability.metrics;

/**
 * Naming constants for the custom business metrics from spec §24 that no auto-instrumentation
 * covers (API latency, HTTP error rate, Kafka lag, DB pool usage, and active-workflow gauges are
 * all emitted automatically once a service depends on actuator + micrometer-registry-prometheus +
 * spring-kafka/HikariCP — see docs/13-observability.md for the full convention and dashboard
 * mapping). Domain code records these directly against the service's {@code MeterRegistry}.
 */
public final class MetricNames {

    private MetricNames() {
    }

    /** Gauge: current count of active/in-flight workflow instances. */
    public static final String WORKFLOWS_ACTIVE = "ensap_workflows_active";

    /** Timer: wall-clock duration of a deployment, start to terminal state. */
    public static final String DEPLOYMENT_DURATION = "ensap_deployment_duration_seconds";

    /** Counter: one increment per deployment outcome. Tag with {@code result=success|failure}. */
    public static final String DEPLOYMENT_RESULT_TOTAL = "ensap_deployment_result_total";

    /** Counter: provider call failures. Tag with {@code provider=<name>}. */
    public static final String PROVIDER_FAILURE_TOTAL = "ensap_provider_failure_total";

    /** Counter: retry attempts. Tag with {@code operation=<name>}. */
    public static final String RETRY_TOTAL = "ensap_retry_total";

    /** Counter: operator/manual interventions required. Tag with {@code reason=<code>}. */
    public static final String MANUAL_INTERVENTION_TOTAL = "ensap_manual_intervention_total";
}
