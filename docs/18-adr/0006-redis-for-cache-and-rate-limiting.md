# ADR-0006: Redis for caching/rate limiting, not durable state

## Status
Accepted (Phase 0)

## Context
Site profile reads are expected to be far more frequent than writes
(dashboards, search), and provider APIs need rate limiting that's
awkward to do purely in-process across multiple service instances
(§14).

## Decision
Use **Redis** for: site profile read caching, other frequently-accessed
read data, API rate limiting, and short-lived coordination. Redis is
explicitly **not** allowed to hold durable workflow or business state —
if Redis is lost, the system must remain fully recoverable from
PostgreSQL/Kafka/Camunda alone (§14).

## Alternatives considered
- **In-memory (JVM heap) caching only.** Rejected: doesn't work once
  there's more than one instance of a service (cache inconsistency
  across instances), and doesn't provide a shared place to do
  cross-instance rate limiting.
- **Database-only rate limiting (a counter table).** Rejected: adds
  write load to PostgreSQL for a high-frequency, low-value-per-write
  concern that Redis is purpose-built for.
- **Using Redis as a message queue or workflow state store.**
  Explicitly rejected — that would violate the "Redis must not be the
  durable source of workflow state" rule (§14) and duplicate what Kafka/
  Camunda already own.

## Consequences
- Every code path that reads from Redis must have a correct fallback to
  PostgreSQL (cache miss = source of truth read, not an error).
- Redis failure is an availability/performance degradation, never a
  correctness bug — this must hold as new caching is added, not just at
  initial design time.
