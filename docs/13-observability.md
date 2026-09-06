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

## Dashboards (Phase 8)

1. Platform Health
2. Deployment Operations
3. Kafka
4. Provider Reliability

## Phase 0 status

Each service exposes a Spring Boot Actuator `/actuator/health` endpoint
(used for the Docker Compose health check) — that is the only
observability surface that exists today. Tracing, metrics export, and
dashboards are Phase 8 work; `shared/common-observability` is a
placeholder module for the OpenTelemetry configuration that phase will
add.
