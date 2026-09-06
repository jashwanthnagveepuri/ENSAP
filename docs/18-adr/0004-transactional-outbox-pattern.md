# ADR-0004: Transactional outbox pattern

## Status
Accepted (Phase 0)

## Context
Naively updating PostgreSQL then publishing to Kafka as two separate
steps has a dual-write problem: if the DB commit succeeds but the Kafka
publish fails (or vice versa, or the process crashes between the two),
state and events diverge and the system becomes silently inconsistent
(§11).

## Decision
Use the **transactional outbox** pattern. The business state change and
an `outbox_event` row insert happen in the **same** PostgreSQL
transaction. A separate publisher process/thread polls `outbox_event`
rows where `published_at IS NULL`, publishes them to Kafka, and marks
them published. If the publisher crashes after a Kafka publish but
before marking the row published, it will re-publish on restart — so
consumers must be idempotent (§12), which the project already requires
for other reasons (duplicate Kafka delivery).

## Alternatives considered
- **Dual write (update DB, then publish).** Rejected — this is exactly
  the problem the outbox pattern exists to solve (§11).
- **Change Data Capture (Debezium reading the WAL).** A legitimate,
  more "automatic" alternative that avoids a polling publisher.
  Rejected for this project: adds another infrastructure component
  (Kafka Connect + Debezium) for a learning project where hand-writing
  the outbox table/publisher is itself the pedagogical point (§3 lists
  "Transactional Outbox" as an explicit learning goal, implying the
  pattern should be visibly implemented, not delegated to CDC tooling).
- **2-phase commit / distributed transaction across DB and Kafka.**
  Rejected: Kafka doesn't participate in XA transactions in a way that's
  practical here, and 2PC is generally avoided in modern distributed
  systems for availability reasons.

## Consequences
- Adds an `outbox_event` table (owned by Deployment Service, per
  `09-database-design.md`) and a publisher component (Phase 3).
- At-least-once delivery to Kafka is the guarantee, not exactly-once —
  every consumer must already be idempotent, which is a project-wide
  rule anyway (§12).
- Publisher lag (time between commit and Kafka publish) is a metric
  worth tracking once Phase 8 observability lands.
