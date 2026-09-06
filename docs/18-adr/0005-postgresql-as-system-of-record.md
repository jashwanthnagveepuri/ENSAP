# ADR-0005: PostgreSQL (local) / Aurora (AWS) as system of record

## Status
Accepted (Phase 0)

## Context
Site, deployment, and audit data need strong consistency, relational
integrity (foreign keys, unique constraints for idempotency), and
transactional guarantees (needed for the outbox pattern, ADR-0004).

## Decision
Use **PostgreSQL** locally and **Aurora PostgreSQL** in AWS (same
engine, same schema/JPA/Hibernate/Liquibase code, §13) as the single
system of record for all three core services, accessed via Spring Data
JPA/Hibernate with Liquibase-managed schema migrations. Each service
logically owns a disjoint set of tables (`09-database-design.md`); no
separate physical database is introduced yet.

## Alternatives considered
- **A NoSQL document store (e.g. DynamoDB/MongoDB).** Rejected: the
  domain is heavily relational (sites → devices → network profiles →
  VLANs/subnets; deployments → steps) and needs uniqueness constraints
  (idempotency keys, `processed_operation`) and transactions (outbox) —
  PostgreSQL is the better fit, and Aurora PostgreSQL is the explicit
  AWS target in the spec (§13).
- **A separate physical database per service from day one.** Considered
  and deferred: true database-per-service is the textbook end-state for
  microservices, but standing up three separate PostgreSQL instances
  adds local-dev friction with no benefit until there's an actual reason
  (independent scaling, independent backup policy) — §13.2 explicitly
  allows enforcing logical ownership through code/docs first and
  physically separating later if warranted.

## Consequences
- Cross-service joins are impossible by design (even though physically
  possible today) — services must go through APIs/events, enforced by
  review against `09-database-design.md`'s ownership table, not by the
  database engine.
- Migrating to physically separate databases later is a mechanical
  Liquibase-changelog-per-service change, not a rewrite, because
  ownership is already logically partitioned.
