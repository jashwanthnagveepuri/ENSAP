# ADR-0001: Coarse-grained service boundaries

## Status
Accepted (Phase 0)

## Context
ENSAP could be split into many tiny services (one per entity type) or a
smaller number of services aligned to business capabilities. The master
spec explicitly warns against "dozens of tiny services" (§7) and against
premature service splitting (§39.9).

## Decision
Use three coarse-grained core services aligned to business capability,
not data entity:

- **Site Profile Service** — owns everything about "what a site looks
  like" (site, device, network profile, WAN circuits, VLANs, subnets).
- **Deployment Service** — owns everything about "the act of deploying"
  (deployment, batch, step, idempotency).
- **Evidence/Audit Service** — owns everything about "what happened and
  proof of it" (evidence, audit events, processed operations).

Provider-specific execution is isolated into **workers**, not services,
because workers are stateless execution units with different scaling/
rate-limit needs per provider (§7.4) — they don't own data or expose an
API, so they don't count as another "service" in the boundary sense.

## Alternatives considered
- **One monolith service.** Rejected: defeats the purpose of a project
  meant to teach service boundaries, independent deployability, and
  Kafka-based decoupling (§3).
- **Entity-per-service** (e.g. separate `device-service`,
  `vlan-service`). Rejected: these entities have no independent
  lifecycle or scaling need from `site` — splitting them would just add
  network hops and distributed transactions for no capability gained.
- **A service per provider** (router-service, switch-service, ...)
  instead of workers. Rejected: providers don't own durable data or need
  an API of their own; a worker (consumer + provider client) is a
  simpler unit that still gets independent scaling/rate-limits (§7.4).

## Consequences
- Each service must expose data other services need via API/events, not
  shared tables (enforced today only by `09-database-design.md` +
  review, since all three currently share one PostgreSQL instance).
- Cross-service consistency is eventual (via outbox/Kafka), not
  transactional — acceptable because the domain (deployment lifecycle)
  is inherently a long-running, step-wise process anyway (§8).
