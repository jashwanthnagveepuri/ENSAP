# 07 — Sequence Diagrams

These describe target behavior for later phases; nothing here is
implemented in Phase 0 (scaffolding only).

## Start a deployment (happy path, Phase 2–4)

```mermaid
sequenceDiagram
    actor Operator
    participant FE as Frontend
    participant DS as Deployment Service
    participant PG as PostgreSQL
    participant CZ as Camunda (Zeebe)
    participant K as Kafka
    participant RW as Router Worker
    participant MR as Mock Router API

    Operator->>FE: Start deployment (siteId)
    FE->>DS: POST /api/deployments (Idempotency-Key)
    DS->>PG: insert deployment + outbox_event (1 txn)
    DS-->>FE: 201 Created {deploymentId}
    DS->>CZ: create process instance (deployment-process)
    Note over PG,K: outbox publisher (separate process/thread)
    PG-->>K: publish deployment.started
    CZ->>RW: job: router-provision
    RW->>K: consume router.provisioning.requested (or job payload)
    RW->>MR: call mock router API
    MR-->>RW: 200 OK
    RW->>K: publish router.provisioning.completed
    RW-->>CZ: complete job
    CZ-->>DS: (continues workflow: switch, wireless, firewall, ...)
```

## Duplicate request / idempotency (Phase 2)

```mermaid
sequenceDiagram
    actor Operator
    participant DS as Deployment Service
    participant PG as PostgreSQL

    Operator->>DS: POST /api/deployments (Idempotency-Key: K1)
    DS->>PG: check idempotency_key K1 → not found
    DS->>PG: insert deployment + idempotency_key K1
    DS-->>Operator: 201 Created {deploymentId=D1}

    Operator->>DS: POST /api/deployments (Idempotency-Key: K1) [retry/duplicate]
    DS->>PG: check idempotency_key K1 → found → D1
    DS-->>Operator: 200 OK {deploymentId=D1} (no new deployment created)
```

## Provider failure → retry exhausted → manual recovery (Phase 5)

```mermaid
sequenceDiagram
    participant CZ as Camunda
    participant RW as Router Worker
    participant MR as Mock Router API
    actor Operator
    participant DS as Deployment Service

    CZ->>RW: job: router-provision (attempt 1)
    RW->>MR: call (behavior=500)
    MR-->>RW: 500
    RW-->>CZ: fail job (retryable)
    CZ->>RW: job: router-provision (attempt 2, backoff+jitter)
    RW->>MR: call (behavior=500)
    MR-->>RW: 500
    RW-->>CZ: fail job (retries exhausted)
    CZ-->>DS: deployment_step = FAILED_REQUIRES_ATTENTION
    Operator->>DS: POST /api/deployments/{id}/retry
    DS->>CZ: signal / retry job
    CZ->>RW: job: router-provision (manual retry)
```

## Duplicate Kafka delivery, idempotent worker (Phase 3/4)

```mermaid
sequenceDiagram
    participant K as Kafka
    participant RW as Router Worker
    participant P as processed_operation (PostgreSQL, Evidence/Audit Svc)
    participant MR as Mock Router API

    K->>RW: router.provisioning.requested (operationId=DEP-001:ROUTER:PROVISION)
    RW->>P: check processed_operation → not found
    RW->>MR: call provider
    MR-->>RW: 200
    RW->>P: insert processed_operation (completed)
    RW->>K: publish router.provisioning.completed

    K->>RW: router.provisioning.requested (same operationId, redelivered)
    RW->>P: check processed_operation → found (completed)
    RW-->>K: publish router.provisioning.completed (no re-execution)
```
