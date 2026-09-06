# 06 — Component Design (Level 3)

## Service internal layering (Spring functional endpoints, §31/§32)

Every Java service follows the same package layout and request flow:

```text
com.ensap.<service>
  config/      RouterConfig.java            — composes routers, wires beans
  router/      <X>Router.java               — RouterFunction<ServerResponse>, HTTP routing only
  handler/     <X>Handler.java              — ServerRequest → ServerResponse translation
  service/     <X>Service.java              — business logic (empty/stubbed in Phase 0)
  repository/  <X>Repository.java           — Spring Data JPA, persistence
  entity/      <X>.java                     — JPA entities (table mapping)
  dto/         <X>Request.java/<X>Response.java — wire types
  exception/   ApiError.java, handlers      — consistent error model (§31)

Request flow:
HTTP → RouterConfig → <X>Router → <X>Handler → <X>Service → <X>Repository → PostgreSQL
```

Routers never contain business logic (§32) — they only map HTTP method +
path to a handler method. Business rules live in `service/`.

## Site Profile Service

Owns: `site`, `device`, `network_profile`, `wan_circuit`, `vlan`,
`subnet` tables and inventory/source-freshness/reconciliation metadata.

Routes (Phase 1 delivers the behavior; Phase 0 delivers the routes as
`501 Not Implemented` stubs):

```text
GET  /api/sites
GET  /api/sites/{siteId}
POST /api/sites/{siteId}/refresh
```

## Deployment Service

Owns: `deployment`, `deployment_batch`, `deployment_step`,
`idempotency_key`, `outbox_event` tables.

```text
GET  /api/deployments
POST /api/deployments                       (Idempotency-Key required)
GET  /api/deployments/{deploymentId}
POST /api/deployments/{deploymentId}/retry
POST /api/deployments/{deploymentId}/cancel
```

Requests the Camunda process `deployment-process` per deployment
(`workflow/camunda/deployment-process.bpmn`) instead of orchestrating
steps itself.

## Evidence/Audit Service

Owns: `deployment_evidence`, `audit_event`, `processed_operation`
tables. Large payloads referenced by S3 key; metadata in PostgreSQL.

```text
GET /api/deployments/{deploymentId}/evidence
GET /api/audit-events
```

## Workers (Phase 4)

Each worker (`router`, `switch`, `wireless`, `firewall`, `ticketing`) is
a small Camunda job worker + Kafka consumer/producer that:

1. Picks up a job (`<provider>-provision` Zeebe job type) or Kafka
   command event.
2. Calls its mock provider API with resilience policies (Phase 5).
3. Publishes a `*.provisioning.completed` / `*.provisioning.failed`
   event and completes/fails the Camunda job.

Workers own no PostgreSQL tables — they are stateless execution units;
`processed_operation` (owned by Evidence/Audit Service) plus the
`operationId` convention (`DEP-001:ROUTER:PROVISION`, §12) is what makes
re-delivery safe.

## Mocks (Phase 4)

Each mock (`mocks/*`) is a minimal HTTP service that returns a
configurable canned response (success/4xx/5xx/timeout/latency/outage,
§22) so workers and resilience code have something realistic — but
controllable — to talk to. The Admin/Provider Simulation frontend page
edits this configuration at runtime.

## Cross-cutting: `shared/`

- `shared/event-contracts` — the Kafka event envelope schema and
  AsyncAPI document (`11-event-catalog.md`), imported/regenerated into
  each service rather than each service inventing its own event shape.
- `shared/common-observability` — placeholder for shared OpenTelemetry/
  logging configuration (Phase 8); not implemented yet.
