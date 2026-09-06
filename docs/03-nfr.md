# 03 — Non-Functional Requirements

## Reliability

- No infinite retries anywhere (API clients, Kafka consumers, workflow
  jobs). Exhausted retries move a step to `FAILED_REQUIRES_ATTENTION`
  for manual recovery (§21).
- Kafka consumers, worker operations, and the deployment API must be
  idempotent — duplicate delivery/requests must be safe (§12).
- Workflow state must survive worker/service restarts (Camunda 8 is the
  durable orchestrator, not an in-memory state machine) (§8).
- If Redis is unavailable, the system must remain correct/recoverable
  from PostgreSQL/Kafka/Camunda — Redis is a cache/accelerator only
  (§14).

## Consistency

- State change + outbox event write happen in a single PostgreSQL
  transaction (transactional outbox, §11) — no dual-write between the
  database and Kafka.
- Each service owns its data; cross-service reads go through APIs or
  events, never direct table access (§7, §13.2).

## Performance (synthetic targets — see §23, not production SLAs)

Design/testing targets, to be validated with actual local benchmarks in
Phase 6 and reported honestly (never assumed):

- ~10,000 sites / ~100,000 devices of synthetic data.
- Batches of up to 500 sites.
- ~500 concurrent deployment workflows.
- Hundreds–thousands of Kafka events/minute.

## Security

- No hard-coded credentials, no secrets committed, no AWS keys baked
  into images (§20).
- Backend enforces RBAC on every mutating endpoint regardless of what
  the frontend renders (§5, §6).
- TLS for external traffic; private subnets for data services in AWS
  (later phase).

## Observability

- Every request/event/workflow instance carries `traceId`,
  `correlationId`, `siteId`, `deploymentId`, `operationId` (§24).
- Structured JSON logs; OpenTelemetry traces; Prometheus metrics (later
  phase — Phase 8).

## Operability

- Every service runs locally via Docker Compose with no AWS credentials
  required (§2.3).
- Every service exposes a health endpoint suitable for container health
  checks and (later) Kubernetes probes.

## Maintainability

- Coarse-grained services (§7.1–7.4); avoid premature service
  splitting (§39.9).
- Functional Spring endpoints only (`RouterFunction`/`HandlerFunction`),
  no annotation-based `@RestController`s, so routing stays declarative
  and centrally composable (§31, §32).
- Documentation (this `docs/` set) is updated alongside architectural
  changes (§39.11).

## Explicitly out of scope for Phase 0

Load testing, security penetration testing, chaos testing, and AWS
deployment are later phases (§37) and are not attempted here.
