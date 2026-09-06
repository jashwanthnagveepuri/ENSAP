# shared/event-contracts

- `asyncapi.yaml` — AsyncAPI 2.6 document for every Kafka topic/event in
  `docs/11-event-catalog.md`.
- `schemas/event-envelope.schema.json` — JSON Schema for the common
  event envelope every message shares.
- `src/main/java/com/ensap/events/` — the Java form of the above: this is
  now a real Maven module (`com.ensap:event-contracts`), consumed by
  `deployment-service` (producer, via the transactional outbox) and
  `evidence-audit-service` (consumer) so the envelope shape and event-type
  strings are defined exactly once.

Phase 3 adds `deployment.cancelled` (both here and in the AsyncAPI/Markdown
catalog) — CANCELLED is a real terminal deployment state that Phase 2 already
implements but Phase 0's catalog draft omitted.

Built as part of the root `pom.xml` reactor (`mvn install` there installs it
so `deployment-service`/`evidence-audit-service` can resolve it even when
built standalone via their own mvnw — see the root pom's description).
