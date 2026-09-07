# deployment-service

Owns deployment/batch/step lifecycle and idempotency keys
(`../../docs/06-component-design.md`). **Phase 2 done**: a deployment
can be created (idempotently) and tracked through a basic Camunda 8
workflow (master spec §37).

## What's implemented (Phase 2)

- `POST /api/deployments` — idempotent create. A repeated
  `Idempotency-Key` returns the original deployment (`200`) instead of
  a duplicate (`201` for a new one); race-safe via the
  `idempotency_key` table's primary key, not locking.
- `GET /api/deployments` (paginated, `status`/`siteId` filter),
  `GET /api/deployments/{id}`.
- `POST /{id}/retry` (only from `FAILED`/`FAILED_REQUIRES_ATTENTION`,
  `409` otherwise) and `POST /{id}/cancel` (`409` from a terminal
  state) — both re-run the same Camunda start/cancel path.
- Deployment state machine: `REQUESTED → RUNNING → COMPLETED`, or
  `→ FAILED` (escalating to `FAILED_REQUIRES_ATTENTION` after 3 failed
  attempts) / `→ CANCELLED`. Tracked via one `deployment_step` row
  (`camunda-workflow-start`) per deployment.
- Camunda 8: `ZeebeClient` (`io.camunda:zeebe-client-java:8.5.9`,
  matching the broker image in `infrastructure/docker/docker-compose.yml`),
  deploys `deployment-process.bpmn` on startup and starts/cancels
  process instances. **Provider job workers are Phase 4** — with no
  worker consuming the BPMN's service tasks, a started deployment's
  process instance sits at `Task_ValidateSite` indefinitely. That's
  expected for Phase 2, not a bug.

### What's intentionally not done here

- **No live-broker requirement.** `deployment.camunda.enabled=false`
  (or the broker simply being unreachable) makes `WorkflowService`
  return a documented `DISABLED`/`ERROR` outcome instead of failing
  the request — the deployment stays `REQUESTED` (disabled) or moves
  to `FAILED`/`FAILED_REQUIRES_ATTENTION` (a real, retryable error).
  The IT suite runs with Camunda disabled (Testcontainers Postgres
  only, no Zeebe testcontainer) — that path is design-verified, not
  faked: `STARTED`/`ERROR` outcomes are covered with a mocked
  `WorkflowService` in `DeploymentServiceTest`; `DISABLED` end-to-end
  in `DeploymentEndToEndIT`.
- **`common-observability` not adopted.** It isn't installed to the
  local Maven repo by this service's own build (no reactor/multi-module
  parent ties the two together yet), so depending on it would make
  `./mvnw verify` fail here unless `shared/common-observability` was
  built first out-of-band — the same reason `site-profile-service`
  (the Phase 1 reference) doesn't consume it either. Revisit once a
  root aggregator POM (or a published/installed artifact) exists.
- **`deployment_processed_operation` table is schema-only**, same status
  as `outbox_event` — Phase 4 job workers populate/consult it for
  per-operation idempotency (master spec §12). Renamed from
  `processed_operation` (003-rename-processed-operation.xml) because
  evidence-audit-service already owns a table of that name on the same
  shared Postgres instance.

## Run locally

```bash
./mvnw spring-boot:run
```

Requires PostgreSQL at `SPRING_DATASOURCE_URL` (defaults to
`localhost:5432/ensap`) and, for real Camunda integration, a Zeebe
broker at `ZEEBE_GATEWAY_ADDRESS` (defaults to `localhost:26500`;
`docker compose up -d postgres zeebe operate` from
`infrastructure/docker/`). Without a broker the service still starts
and serves requests — see "What's intentionally not done here" above.

## Build & test

```bash
./mvnw -q verify
```

Runs the Phase 0 H2 context-load smoke test, `DeploymentServiceTest`
(Mockito unit tests), and `DeploymentEndToEndIT` (Testcontainers
Postgres, via `maven-failsafe-plugin` — without it `*IT` classes
silently never run).

## API contract

`src/main/resources/openapi/deployment-service.yaml`

## Package layout

Functional endpoints per master spec §31/§32 — see
`../../docs/06-component-design.md` for the shared layering across all
three services. `workflow/` wraps the Zeebe client
(`WorkflowService`, `ZeebeConfig`, `WorkflowResourceDeployer`); the
packaged copy of `workflow/camunda/deployment-process.bpmn` lives at
`src/main/resources/camunda/` so it deploys from the built jar.
