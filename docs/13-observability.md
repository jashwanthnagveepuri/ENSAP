# 13 — Observability (target design; implemented in Phase 8)

## Correlation metadata (§24)

Every request, workflow instance, and event carries: `traceId`,
`correlationId`, `siteId`, `deploymentId`, `workflowInstanceId`,
`operationId` — enabling one deployment to be traced end-to-end across
API → Camunda → Kafka → worker → mock provider.

## Stack

- **OpenTelemetry** — distributed tracing (instrumentation added per
  service in Phase 8), exported to a local collector.
- **Prometheus** — metrics scraping.
- **Grafana** — dashboards.
- **CloudWatch** — AWS-native metrics/logs once deployed (Phase 10).
- **Structured JSON logging** — every service logs JSON with the
  correlation fields above, not free-text.

## Metrics to track (§24)

API latency, request rate, HTTP error rate, Kafka consumer lag,
database connection pool usage, active workflow count, deployment
duration, deployment success/failure rate, provider failure rate, retry
count, manual intervention count.

Of these, API latency/request rate/error rate (`http_server_requests_seconds`),
Kafka consumer lag, and DB connection-pool usage are emitted automatically by
Spring Boot's own Micrometer auto-instrumentation once a service depends on
`spring-boot-starter-actuator` + `micrometer-registry-prometheus` (and
`spring-kafka` / a JDBC datasource, respectively) — no application code needed.

The remaining metrics are business-specific and must be recorded explicitly by
domain code against the service's `MeterRegistry`. Naming convention: lowercase
snake_case, `ensap_` prefix, unit suffix (`_seconds`, `_total`), following
[Prometheus naming best practices](https://prometheus.io/docs/practices/naming/):

| Metric | Type | Name | Tags |
|---|---|---|---|
| Active workflow count | gauge | `ensap_workflows_active` | — |
| Deployment duration | timer | `ensap_deployment_duration_seconds` | — |
| Deployment success/failure | counter | `ensap_deployment_result_total` | `result=success\|failure` |
| Provider failure rate | counter | `ensap_provider_failure_total` | `provider=<name>` |
| Retry count | counter | `ensap_retry_total` | `operation=<name>` |
| Manual intervention count | counter | `ensap_manual_intervention_total` | `reason=<code>` |

These names are also available as constants in
`shared/common-observability`'s `com.ensap.observability.metrics.MetricNames`,
so services don't hand-type the strings.

## Dashboards (Phase 8)

1. **Platform Health** — request rate, HTTP error rate, API latency (p50/p95/p99)
   from `http_server_requests_seconds`; JVM/DB connection-pool panels from the
   auto-instrumented actuator metrics.
2. **Deployment Operations** — `ensap_deployment_duration_seconds` and
   `ensap_deployment_result_total` (success/failure rate), `ensap_workflows_active`,
   `ensap_manual_intervention_total`.
3. **Kafka** — consumer lag and throughput from Spring Kafka's auto-bound
   Micrometer metrics, per topic/consumer-group.
4. **Provider Reliability** — `ensap_provider_failure_total` by provider,
   `ensap_retry_total` by operation.

## Correlation-id propagation and structured logging

`shared/common-observability` provides the buildable Maven module every
service depends on to get correlation-id propagation and structured JSON
logging (see that module's README for the adoption steps):

- `CorrelationIdWebFilter` reads/generates `traceId`'s sibling fields —
  `correlationId`, `siteId`, `deploymentId`, `workflowInstanceId`, `operationId`
  — from request headers, echoes `correlationId` on the response, and
  propagates all of them through the reactive chain into SLF4J MDC (so plain
  `log.info(...)` calls anywhere in a WebFlux handler pick them up). `traceId`
  itself is populated by Micrometer Tracing (`micrometer-tracing-bridge-otel`),
  not by this filter.
- Structured JSON logs come from Spring Boot's built-in structured logging
  support (`logging.structured.format.console: ecs`, no custom Logback config)
  — every MDC entry, correlation fields included, is emitted as a JSON field.

## Phase 0 status

Each service exposes a Spring Boot Actuator `/actuator/health` endpoint
(used for the Docker Compose health check) — that is the only observability
surface that existed at Phase 0. `shared/common-observability` (built ahead of
Phase 8, in parallel with other Phase 0/early work) is the reusable library;
wiring it into each service, adding the OTel collector, and building the four
Grafana dashboards above is the remaining Phase 8 work.
