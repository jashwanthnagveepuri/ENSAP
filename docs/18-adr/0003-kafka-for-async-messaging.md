# ADR-0003: Kafka for async messaging, `siteId` as partition key

## Status
Accepted (Phase 0)

## Context
Services and workers need to communicate without the sender blocking on
the consumer (§9) — e.g. the Deployment Service should not hold an HTTP
call open while the Router Worker calls a provider API. Something has to
carry `*.provisioning.requested/completed/failed` events at scale
between decoupled components, and per-site event ordering matters (e.g.
a site shouldn't process `router.completed` before `router.requested`).

## Decision
Use **Apache Kafka** locally (Amazon MSK in AWS, same protocol) for all
asynchronous domain events (catalog in `11-event-catalog.md`). Partition
every deployment-related topic by **`siteId`** so all events for one
site land on the same partition and are processed in order relative to
each other, while different sites parallelize freely across partitions.

## Alternatives considered
- **RabbitMQ / SQS.** Rejected: simpler queueing semantics are fine for
  point-to-point work distribution but don't give the same
  replay/consumer-group/ordering-by-key model that the multi-consumer,
  partition-ordered event catalog here relies on, and Kafka is the
  explicit learning goal (§3).
- **Partition by `deploymentId`** instead of `siteId`. Rejected:
  multiple deployments can target the same site over time (redeploys),
  and cross-deployment ordering for a site (e.g. don't let a stale retry
  race a newer deployment) is the property that actually matters
  operationally.
- **No partition key (round-robin).** Rejected: gives up ordering
  entirely, which would let a `router.provisioning.failed` be processed
  before its own `router.provisioning.requested` under concurrent
  consumers — unacceptable for a system that already requires
  idempotent, order-sensitive consumers (§9, §12).

## Consequences
- Consumers must still be idempotent regardless of ordering (duplicate
  delivery is still possible, §9) — partitioning fixes ordering, not
  exactly-once delivery.
- Kafka is explicitly **not** the system of record and **not** a
  substitute for Camunda workflow state (§9) — durable truth lives in
  PostgreSQL (via outbox) and Camunda.
- Hot sites (frequent redeploys) could create partition skew; not a
  concern at the synthetic scale targets in §23, revisited if Phase 6
  benchmarking shows otherwise.
