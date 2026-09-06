# 15 — Disaster Recovery (target design)

This is a learning project; "disaster recovery" here means demonstrating
the recovery properties the architecture is supposed to provide, and
documenting them honestly — not a production DR runbook with real RTO/RPO
commitments.

## What must survive component loss

| Component lost | System stays correct because |
|---|---|
| A worker process crashes mid-operation | Camunda job is not completed → Zeebe re-offers it; `processed_operation`/`operationId` prevents double-execution on retry (§12, §21). |
| A core service restarts | No workflow state lives in service memory — Camunda holds workflow state, PostgreSQL holds row state (§8, §34). |
| Redis is lost entirely | Redis is cache/rate-limit only; the system must remain correct by re-reading PostgreSQL — no workflow or business state is Redis-only (§14). |
| Kafka broker restarts / consumer lag spikes | Kafka is transport, not the system of record; the transactional outbox guarantees no event is silently lost — a delayed consumer just processes late (§9, §11). |
| PostgreSQL fails mid-transaction | Business-state-change + outbox-event write are one transaction — either both happen or neither does (§11). |
| A provider API has a temporary outage | Bounded retry + circuit breaker + `FAILED_REQUIRES_ATTENTION` for manual recovery — never infinite retry (§21). |

## Backups (later phase, not yet configured)

Local: none — this is a disposable dev environment (`docker compose
down -v` is an acceptable reset). AWS (Phase 10): Aurora automated
backups/snapshots, S3 versioning for evidence.

## Explicitly not covered by this project

Multi-region failover, real RTO/RPO SLAs, and production incident
response — out of scope for a portfolio/learning project (§41).
