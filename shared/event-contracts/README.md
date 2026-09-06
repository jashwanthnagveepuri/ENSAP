# shared/event-contracts

- `asyncapi.yaml` — AsyncAPI 2.6 document for every Kafka topic/event in
  `docs/11-event-catalog.md`.
- `schemas/event-envelope.schema.json` — JSON Schema for the common
  event envelope every message shares.

Phase 0: contracts only. No producer/consumer code references these
yet — that begins in Phase 3 (outbox + Kafka).
